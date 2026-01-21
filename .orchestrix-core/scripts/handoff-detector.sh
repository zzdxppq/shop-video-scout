#!/bin/bash
# Orchestrix HANDOFF Detector - tmux Automation Hook (MCP Version)
# Triggers on Claude Code Stop event, detects HANDOFF and routes to target agent
#
# Design principles:
# - NO dependency on environment variables (robust)
# - Scans ALL tmux windows to find HANDOFF message
# - Uses hash-based deduplication to prevent re-processing
# - Background process handles cleanup and lock release
#
# Pro/Team Feature: This script is only available for Pro and Team subscribers.

set +e  # Don't exit on errors

# Fix PATH for homebrew (Apple Silicon & Intel Macs)
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

SESSION_NAME="${ORCHESTRIX_SESSION:-}"
# Initial log file - will be updated once session is found
LOG_FILE="/tmp/orchestrix-init-handoff.log"

get_agent_name() {
    case "$1" in
        0) echo "architect" ;;
        1) echo "sm" ;;
        2) echo "dev" ;;
        3) echo "qa" ;;
        *) echo "" ;;
    esac
}

get_window_num() {
    case "$1" in
        architect) echo "0" ;;
        sm) echo "1" ;;
        dev) echo "2" ;;
        qa) echo "3" ;;
        *) echo "" ;;
    esac
}

get_agent_command() {
    case "$1" in
        architect) echo "/o architect" ;;
        sm) echo "/o sm" ;;
        dev) echo "/o dev" ;;
        qa) echo "/o qa" ;;
        *) echo "" ;;
    esac
}

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"; }

log "========== Hook triggered =========="

if [[ -z "$SESSION_NAME" ]]; then
    # Support both pod sessions (u{userId}-bp-{blueprintId}) and local orchestrix sessions (orchestrix-*)
    # Multi-user support: Session naming changed from blueprint-* to u*-bp-*
    SESSION_NAME=$(tmux list-sessions -F '#{session_name}' 2>/dev/null | grep -E '^(u[a-f0-9-]+-bp-|orchestrix-)' | head -1)
fi

if [[ -z "$SESSION_NAME" ]]; then
    log "EXIT: No orchestrix session found"
    exit 0
fi

if ! tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
    log "EXIT: Session '$SESSION_NAME' not found"
    exit 0
fi

LOG_FILE="/tmp/orchestrix-${SESSION_NAME}-handoff.log"
log "Session: $SESSION_NAME"

PROCESSED_FILE="/tmp/orchestrix-${SESSION_NAME}-processed.txt"
touch "$PROCESSED_FILE"

SOURCE_WIN=""
TARGET=""
CMD=""
HANDOFF_HASH=""

for win in 0 1 2 3; do
    OUTPUT=$(tmux capture-pane -t "$SESSION_NAME:$win" -p -S -100 2>/dev/null)
    [[ -z "$OUTPUT" ]] && continue

    LINE=$(echo "$OUTPUT" | grep -E 'ð¯.*HANDOFF.*TO' | tail -1)
    [[ -z "$LINE" ]] && continue

    HASH=$(echo "$LINE" | md5 2>/dev/null || echo "$LINE" | md5sum 2>/dev/null | cut -d' ' -f1)

    if grep -q "$HASH" "$PROCESSED_FILE" 2>/dev/null; then
        continue
    fi

    if [[ "$LINE" =~ HANDOFF[[:space:]]+TO[[:space:]]+([a-zA-Z0-9_-]+):[[:space:]]*\*?([a-z0-9-]+)([[:space:]]+([0-9]+\.[0-9]+))? ]]; then
        SOURCE_WIN=$win
        TARGET=$(echo "${BASH_REMATCH[1]}" | tr '[:upper:]' '[:lower:]')
        CMD="*${BASH_REMATCH[2]}${BASH_REMATCH[4]:+ ${BASH_REMATCH[4]}}"
        HANDOFF_HASH=$HASH
        log "Found HANDOFF in window $win: $LINE"
        break
    fi
done

if [[ -z "$TARGET" || -z "$CMD" ]]; then
    log "No HANDOFF in terminal output, checking fallback file..."

    FALLBACK_FILE=""
    for win in 0 1 2 3; do
        PANE_DIR=$(tmux display-message -t "$SESSION_NAME:$win" -p '#{pane_current_path}' 2>/dev/null)
        if [[ -n "$PANE_DIR" && -f "$PANE_DIR/.orchestrix-core/runtime/pending-handoff.json" ]]; then
            FALLBACK_FILE="$PANE_DIR/.orchestrix-core/runtime/pending-handoff.json"
            SOURCE_WIN=$win
            break
        fi
    done

    if [[ -z "$FALLBACK_FILE" ]]; then
        log "No pending-handoff.json found"
        exit 0
    fi

    log "Found fallback file: $FALLBACK_FILE"

    if command -v jq &>/dev/null; then
        STATUS=$(jq -r '.status // "unknown"' "$FALLBACK_FILE" 2>/dev/null)
        if [[ "$STATUS" != "pending" ]]; then
            log "Fallback file status is '$STATUS', not 'pending'. Skipping."
            exit 0
        fi
        TARGET=$(jq -r '.target_agent // ""' "$FALLBACK_FILE" 2>/dev/null)
        CMD=$(jq -r '.command // ""' "$FALLBACK_FILE" 2>/dev/null)
        STORY_ID=$(jq -r '.story_id // ""' "$FALLBACK_FILE" 2>/dev/null)
        SOURCE_AGENT=$(jq -r '.source_agent // ""' "$FALLBACK_FILE" 2>/dev/null)
    else
        STATUS=$(grep -o '"status"[[:space:]]*:[[:space:]]*"[^"]*"' "$FALLBACK_FILE" | sed 's/.*"\([^"]*\)"$/\1/')
        if [[ "$STATUS" != "pending" ]]; then
            log "Fallback file status is '$STATUS', not 'pending'. Skipping."
            exit 0
        fi
        TARGET=$(grep -o '"target_agent"[[:space:]]*:[[:space:]]*"[^"]*"' "$FALLBACK_FILE" | sed 's/.*"\([^"]*\)"$/\1/')
        CMD=$(grep -o '"command"[[:space:]]*:[[:space:]]*"[^"]*"' "$FALLBACK_FILE" | sed 's/.*"\([^"]*\)"$/\1/')
        STORY_ID=$(grep -o '"story_id"[[:space:]]*:[[:space:]]*"[^"]*"' "$FALLBACK_FILE" | sed 's/.*"\([^"]*\)"$/\1/')
        SOURCE_AGENT=$(grep -o '"source_agent"[[:space:]]*:[[:space:]]*"[^"]*"' "$FALLBACK_FILE" | sed 's/.*"\([^"]*\)"$/\1/')
    fi

    if [[ -z "$TARGET" || -z "$CMD" ]]; then
        log "Invalid fallback file content"
        exit 0
    fi

    HANDOFF_HASH=$(echo "fallback-$SOURCE_AGENT-$TARGET-$CMD-$STORY_ID" | md5 2>/dev/null || echo "fallback-$SOURCE_AGENT-$TARGET-$CMD-$STORY_ID" | md5sum 2>/dev/null | cut -d' ' -f1)

    if grep -q "$HANDOFF_HASH" "$PROCESSED_FILE" 2>/dev/null; then
        log "Fallback handoff already processed"
        exit 0
    fi

    SOURCE_WIN=$(get_window_num "$SOURCE_AGENT")

    log "[FALLBACK] Recovered handoff from file: $SOURCE_AGENT (win $SOURCE_WIN) -> $TARGET: $CMD"

    if command -v jq &>/dev/null; then
        jq --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '.status = "completed_by_fallback" | .completed_at = $ts' "$FALLBACK_FILE" > "$FALLBACK_FILE.tmp" 2>/dev/null && mv "$FALLBACK_FILE.tmp" "$FALLBACK_FILE"
    else
        sed -i.bak 's/"status"[[:space:]]*:[[:space:]]*"pending"/"status": "completed_by_fallback"/' "$FALLBACK_FILE" 2>/dev/null
    fi
fi

echo "$HANDOFF_HASH" >> "$PROCESSED_FILE"
tail -100 "$PROCESSED_FILE" > "$PROCESSED_FILE.tmp" 2>/dev/null && mv "$PROCESSED_FILE.tmp" "$PROCESSED_FILE"

if [[ -z "$SOURCE_AGENT" ]]; then
    SOURCE_AGENT=$(get_agent_name "$SOURCE_WIN")
fi
TARGET_WIN=$(get_window_num "$TARGET")

if [[ -z "$TARGET_WIN" ]]; then
    log "ERROR: Unknown target agent '$TARGET'"
    exit 0
fi

if [[ "$TARGET_WIN" == "$SOURCE_WIN" ]]; then
    log "ERROR: Source and target are same window"
    exit 0
fi

log "HANDOFF: $SOURCE_AGENT (win $SOURCE_WIN) -> $TARGET (win $TARGET_WIN)"
log "Command: $CMD"

LOCK="/tmp/orchestrix-${SESSION_NAME}-${SOURCE_WIN}.lock"
LOCK_TIMEOUT=60

if ! mkdir "$LOCK" 2>/dev/null; then
    if [[ -f "$LOCK/ts" ]]; then
        ts=$(cat "$LOCK/ts" 2>/dev/null || echo 0)
        now=$(date +%s)
        age=$((now - ts))
        if [[ $age -lt $LOCK_TIMEOUT ]]; then
            log "SKIP: Window $SOURCE_WIN locked (${age}s ago)"
            exit 0
        fi
        log "Stale lock (${age}s), cleaning"
    fi
    rm -rf "$LOCK" 2>/dev/null
    mkdir "$LOCK" 2>/dev/null || { log "SKIP: lock race"; exit 0; }
fi
date +%s > "$LOCK/ts"

log "Sending '$CMD' to $TARGET (window $TARGET_WIN)..."

if tmux send-keys -t "$SESSION_NAME:$TARGET_WIN" "$CMD" 2>/dev/null; then
    sleep 0.5
    tmux send-keys -t "$SESSION_NAME:$TARGET_WIN" Enter
    log "SUCCESS: Command sent to $TARGET"
else
    log "ERROR: Failed to send command"
    rm -rf "$LOCK"
    exit 0
fi

RELOAD_CMD=$(get_agent_command "$SOURCE_AGENT")

(
    log "[BG] Starting cleanup for $SOURCE_AGENT (window $SOURCE_WIN)"
    sleep 2

    tmux send-keys -t "$SESSION_NAME:$SOURCE_WIN" "/clear" 2>/dev/null
    sleep 0.5
    tmux send-keys -t "$SESSION_NAME:$SOURCE_WIN" Enter
    log "[BG] /clear sent to $SOURCE_AGENT"

    sleep 5

    if [[ -n "$RELOAD_CMD" ]]; then
        tmux send-keys -t "$SESSION_NAME:$SOURCE_WIN" "$RELOAD_CMD" 2>/dev/null
        sleep 0.5
        tmux send-keys -t "$SESSION_NAME:$SOURCE_WIN" Enter
        log "[BG] Reload sent: $RELOAD_CMD"
        sleep 15
    fi

    if [[ -n "$HANDOFF_HASH" && -f "$PROCESSED_FILE" ]]; then
        grep -v "^${HANDOFF_HASH}$" "$PROCESSED_FILE" > "$PROCESSED_FILE.tmp" 2>/dev/null && mv -f "$PROCESSED_FILE.tmp" "$PROCESSED_FILE"
        log "[BG] Hash removed from processed file: $HANDOFF_HASH"
    fi

    rm -rf "$LOCK"
    log "[BG] Cleanup complete, lock released"
) >> "$LOG_FILE" 2>&1 &

log "Background process started (PID $!)"
log "========== Hook complete =========="
exit 0
