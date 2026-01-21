#!/bin/bash
# ACP Stop Hook - Sends agent output to Gateway for LLM-based orchestration
set +e

GATEWAY_URL="${ACP_GATEWAY_URL:-https://ws.youlidao.ai}"
WEBHOOK_SECRET_FILE="/Users/youlidao/o/webhook-secret.txt"
[[ -f "$WEBHOOK_SECRET_FILE" ]] && WEBHOOK_SECRET=$(cat "$WEBHOOK_SECRET_FILE" | tr -d '\n') || WEBHOOK_SECRET="${WEBHOOK_SECRET:-}"
LOG_FILE="/tmp/acp-stop-hook.log"
CAPTURE_LINES=500

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG_FILE"; }
log "========== ACP Stop Hook triggered =========="

[[ -z "$WEBHOOK_SECRET" ]] && { log "ERROR: WEBHOOK_SECRET not configured"; exit 0; }
command -v curl &>/dev/null || { log "ERROR: curl not found"; exit 0; }

SESSION_NAME="${ORCHESTRIX_SESSION:-}"
[[ -z "$SESSION_NAME" ]] && SESSION_NAME=$(tmux list-sessions -F '#{session_name}' 2>/dev/null | grep -E '^(u[a-f0-9-]+-bp-|orchestrix-)' | head -1)
[[ -z "$SESSION_NAME" ]] && { log "EXIT: No session found"; exit 0; }
log "Session: $SESSION_NAME"

[[ "$SESSION_NAME" =~ ^u[a-f0-9-]+-bp-([a-f0-9-]+)$ ]] && BLUEPRINT_ID="${BASH_REMATCH[1]}"
[[ -z "$BLUEPRINT_ID" ]] && { log "ERROR: Cannot determine blueprint_id"; exit 0; }

get_agent_id() { case "$1" in 0) echo "architect";; 1) echo "sm";; 2) echo "dev";; 3) echo "qa";; esac; }
AGENT_ID="${AGENT_ID:-}"
for win in 0 1 2 3; do
    [[ -n "$AGENT_ID" ]] && break
    OUTPUT=$(tmux capture-pane -t "$SESSION_NAME:$win" -p -S -50 2>/dev/null)
    echo "$OUTPUT" | grep -q "HANDOFF" && { AGENT_ID=$(get_agent_id "$win"); CURRENT_WIN=$win; log "Found HANDOFF in window $win"; }
done
[[ -z "$AGENT_ID" ]] && { log "ERROR: Cannot determine agent_id"; exit 0; }

OUTPUT=$(tmux capture-pane -t "$SESSION_NAME:${CURRENT_WIN:-0}" -p -S -"$CAPTURE_LINES" 2>/dev/null)
log "Captured output: ${#OUTPUT} characters"

ESCAPED_OUTPUT=$(echo "$OUTPUT" | jq -Rs '.' 2>/dev/null || echo '""')
PAYLOAD="{\"event_type\":\"agent_output\",\"blueprint_id\":\"$BLUEPRINT_ID\",\"agent_id\":\"$AGENT_ID\",\"content\":$ESCAPED_OUTPUT,\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}"

log "Sending webhook to $GATEWAY_URL/api/hook"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -H "X-Webhook-Secret: $WEBHOOK_SECRET" -d "$PAYLOAD" --max-time 10 "$GATEWAY_URL/api/hook" 2>&1)
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
[[ "$HTTP_CODE" == "200" ]] && log "SUCCESS: Webhook sent" || log "ERROR: Webhook failed (HTTP $HTTP_CODE)"
log "========== ACP Stop Hook complete =========="
exit 0