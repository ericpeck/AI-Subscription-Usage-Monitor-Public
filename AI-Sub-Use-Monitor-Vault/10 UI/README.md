# 10 UI

All design and screen-spec files live here. Do not edit these unless Eric asks. Agent rules stay in [[AGENTS]].

| File | What it is | Authority |
|---|---|---|
| [[IMPLEMENTATION_OUTLINE]] | Build spec, rev 2, 4 Sep 2026 | **How to build.** |
| `ai-platform-usage-tracker/project/Screens Board.dc.html` | Full board, 22 screens | **Design of record.** Match the visual output; do not port the DOM. |
| `ai-platform-usage-tracker/project/Screens Board-print.dc.html` | Design rationale and colour table | Read. Cursor's print-table red is stale — follow the board (graphite). |
| `Design.pdf` | 9-page visual of some screens | Reference. Image-only PDF. Page 2 is the Claude 1e header. |
| `1stDraft-Screens-Board.html` | Earlier snapshot of the board | History. Not authority. |
| `ai-platform-usage-tracker/project/Usage.dc.html` | Earlier 5-platform prototype | Carries the only working state logic: breach at `usage >= 90`, pace copy. |
| `ai-platform-usage-tracker/project/_ds/broadsheet-…/styles.css` | Broadsheet tokens | Token source. Ignore `.halftone` through `.cmyk-head`. |
| `ai-platform-usage-tracker/project/screenshots/*.png` | Visual acceptance targets | Present in this vault copy. |
| `AI Platform usage tracker-handoff.zip` | Original Claude Design export | Duplicate of the extracted folder. Gitignored. |
