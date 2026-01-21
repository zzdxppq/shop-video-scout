#!/bin/bash
# Orchestrix tmux Multi-Agent Session Starter (MCP Version)
# Pro/Team Feature: This script is only available for Pro and Team subscribers.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "Working directory: $WORK_DIR"

CONFIG_FILE="$WORK_DIR/.orchestrix-core/core-config.yaml"
REPO_ID=""

if [ -f "$CONFIG_FILE" ]; then
    REPO_ID=$(grep -E "^\s*repository_id:" "$CONFIG_FILE" 2>/dev/null | head -1 | sed "s/.*repository_id:[[:space:]]*['\"]*//" | sed "s/['\"].*//")
    REPO_ID=$(echo "$REPO_ID" | tr -d "'" | tr -d '"' | tr -d ' ')
fi

if [ -z "$REPO_ID" ]; then
    REPO_ID=$(basename "$WORK_DIR")
    echo "Warning: No repository_id in config, using directory name: $REPO_ID"
fi

REPO_ID=$(echo "$REPO_ID" | tr -cd 'a-zA-Z0-9_-')
SESSION_NAME="orchestrix-${REPO_ID}"
LOG_FILE="/tmp/orchestrix-${REPO_ID}-handoff.log"

echo "Repository ID: $REPO_ID"
echo "tmux Session: $SESSION_NAME"
echo "Log file: $LOG_FILE"

if ! command -v tmux &> /dev/null; then
    echo "Error: tmux is not installed. Please run: brew install tmux"
    exit 1
fi

if ! command -v cc &> /dev/null; then
    echo "Error: cc command not available. Please configure alias: alias cc='claude'"
    exit 1
fi

if tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
    echo "Warning: Session '$SESSION_NAME' exists, closing..."
    tmux kill-session -t "$SESSION_NAME"
fi

echo "Creating tmux session: $SESSION_NAME"
tmux new-session -d -s "$SESSION_NAME" -n "Arch" -c "$WORK_DIR"
tmux set-option -t "$SESSION_NAME" status-left-length 20
tmux set-option -t "$SESSION_NAME" status-right-length 60

declare -a AGENTS=("architect" "sm" "dev" "qa")
declare -a WINDOW_NAMES=("Arch" "SM" "Dev" "QA")

for i in 0 1 2 3; do
    if [ "$i" -gt 0 ]; then
        tmux new-window -t "$SESSION_NAME:$i" -n "${WINDOW_NAMES[$i]}" -c "$WORK_DIR"
    fi
    tmux send-keys -t "$SESSION_NAME:$i" "export AGENT_ID=${AGENTS[$i]}" C-m
    tmux send-keys -t "$SESSION_NAME:$i" "export ORCHESTRIX_SESSION=$SESSION_NAME" C-m
    tmux send-keys -t "$SESSION_NAME:$i" "export ORCHESTRIX_LOG=$LOG_FILE" C-m
    tmux send-keys -t "$SESSION_NAME:$i" "clear && echo '${WINDOW_NAMES[$i]} Agent (Window $i)'" C-m
done

CC_STARTUP_WAIT=60
AGENT_ACTIVATION_DELAY=2
AGENT_LOAD_WAIT=15

echo "Starting Claude Code in all windows..."
for i in 0 1 2 3; do
    tmux send-keys -t "$SESSION_NAME:$i" "cc" C-m
done

echo "Waiting ${CC_STARTUP_WAIT}s for Claude Code to start..."
sleep "$CC_STARTUP_WAIT"

echo "Auto-activating agents..."
for i in 0 1 2 3; do
    tmux send-keys -t "$SESSION_NAME:$i" "/o ${AGENTS[$i]}"
    sleep 1
    tmux send-keys -t "$SESSION_NAME:$i" "Enter"
    [ "$i" -lt 3 ] && sleep "$AGENT_ACTIVATION_DELAY"
done

echo "Waiting ${AGENT_LOAD_WAIT}s for agents to load..."
sleep "$AGENT_LOAD_WAIT"

echo "Starting workflow in SM window..."
tmux send-keys -t "$SESSION_NAME:1" "1"
sleep 1
tmux send-keys -t "$SESSION_NAME:1" "Enter"

tmux select-window -t "$SESSION_NAME:1"

echo ""
echo "==============================================="
echo "Orchestrix automation started!"
echo "==============================================="
echo "Window Layout: 0-Architect, 1-SM (current), 2-Dev, 3-QA"
echo "Navigation: Ctrl+b -> 0/1/2/3 | n/p | d | ["
echo "Monitor: tail -f $LOG_FILE"
echo "Reconnect: tmux attach -t $SESSION_NAME"
echo ""

tmux attach-session -t "$SESSION_NAME"
