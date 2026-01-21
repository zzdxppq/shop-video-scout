---
description: Activate Orchestrix Agent (e.g., /o dev, /o sm, /o qa)
---

# Orchestrix Agent Activation

**CRITICAL**: You MUST use `ReadMcpResourceTool` (NOT prompts!) with the EXACT parameters below.

## Step 1: Read Agent Configuration

Call `ReadMcpResourceTool` with these EXACT parameters:
- **server**: `orchestrix`
- **uri**: `orchestrix://agents/$ARGUMENTS.yaml`

Example for `/o pm`:
- server: `orchestrix`
- uri: `orchestrix://agents/pm.yaml`

**DO NOT** use `orchestrix://prompts/` - agents are exposed as **resources**, not prompts!

## Step 2: After Loading Agent

1. Adopt the persona defined in the `agent` section completely
2. Follow `activation_instructions` exactly
3. Display greeting with agent name/role
4. Show the numbered command list from `commands.help.output_format`
5. Wait for user selection

## Available Agents

| ID | Agent | Description |
|----|-------|-------------|
| `dev` | Full Stack Developer | implementation, debugging, refactoring |
| `sm` | Scrum Master | story creation, epic management, agile guidance |
| `qa` | QA Engineer | E2E testing, quality verification |
| `architect` | Solution Architect | system design, tech selection, API design |
| `pm` | Product Manager | PRDs, product strategy, roadmap planning |
| `po` | Product Owner | backlog management, story refinement |
| `analyst` | Business Analyst | market research, competitive analysis |
| `ux-expert` | UX Expert | UI/UX design, wireframes, prototypes |
| `orchestrix-orchestrator` | Workflow Coordinator | multi-agent tasks |
| `orchestrix-master` | Master Agent | one-off tasks across domains |
| `decision-evaluator` | Decision Evaluator | execute decision rules |

## Action Required

If `$ARGUMENTS` is empty, show the table above and ask user to select.

Otherwise, execute NOW:
```
ReadMcpResourceTool(server="orchestrix", uri="orchestrix://agents/$ARGUMENTS.yaml")
```
