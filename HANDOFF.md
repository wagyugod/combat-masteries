# Handoff: PvM Performance Hub + Combat Masteries

Written 2026-11-06 by the cloud Claude Code session that built both plugins, for a local agent taking over
on the user's PC. The same file is committed in both repos. Everything below was true at the time of
writing; when in doubt, trust the code and the git log over this file.

---

## 1. The two projects at a glance

| | PvM Performance Hub | Combat Masteries |
|---|---|---|
| What | Measures how well you use your ticks in boss fights (lost ticks, gaps, potted/prayed hits, spec, thrall, off-prayer hits). Chat summary per kill, live overlay, OSRS-styled side panel with history and graphs. | An achievement system beyond Grandmaster Combat Achievements: speed/challenge tasks, tiers (Ascended / Exalted / Immortal), a glowing-eyes cosmetic for the Tzkal Slayer Helmet, and an in-game window that copies the Combat Achievements window. |
| GitHub | `wagyugod/pvm-performance-tracker` (repo kept its old name) | `wagyugod/combat-masteries` (public) |
| Working branch | `claude/kind-brahmagupta-fspgga` | `claude/kind-brahmagupta-fspgga` |
| PR | #1 (draft, open, base `main`) — all work lives here, never merged | #1 merged into `main`; #2 (draft, open): hub listing `icon.png` + tidy `runelite-plugin.properties` |
| User's local folder | `~\pvm-performance-hub` (`C:\Users\12039\pvm-performance-hub`) | `~\combat-masteries` |
| Package / internalName / config group | `com.pvmperformance` / `pvm-performance-hub` / `pvmperformancehub` | `com.combatmasteries` / see `CombatMasteriesPlugin` / see `CombatMasteriesConfig` |
| Build | `./gradlew build --no-daemon -q --max-workers=1` → 225 tests, 0 failures at handoff | same command → 117 tests at handoff |
| Status | Never released. Heavily tested in-game by the user (Phosani, Zulrah, Vardorvis); 10 new bosses added at the end, untested. | Merged to main, public repo, close to Plugin Hub submission; needs the user's final review. |

Author for both: **wagyugod** (user email nbed@umich.edu — use only for attribution). Licence BSD-2.

### Launch commands (PowerShell, user's PC — also pinned on claude.ai as "Plugin Launch Commands")
Close RuneLite first. One line each:

```
cd ~\combat-masteries; git pull origin claude/kind-brahmagupta-fspgga; .\gradlew.bat run
cd ~\pvm-performance-hub; git pull origin claude/kind-brahmagupta-fspgga; .\gradlew.bat run
```

Gotchas the user hit: `git clone … ~\folder` creates a literal folder named `~` (git doesn't expand `~`; use
`"$HOME\folder"`); `powershell -File ~\…` also doesn't expand `~`; a PowerShell window opened before
`winget install` doesn't see the new PATH. A RuneLite that wasn't restarted after a pull is still running the
old build (the debug file name in chat tells you — it changes on every restart + `::pvmdebug`).

---

## 2. Working with this user (read this first)

These rules come from real, painful mistakes. Follow them.

- **Be brief.** The user asked repeatedly for short answers ("1 sentence", "make it brief"). No essays, no
  recaps, no listing options you won't pursue. Lead with the answer.
- **Never push a guess as a fix.** "Why are you putting in fixes to unsolved problems?" — if the evidence
  (a debug log line, a screenshot, the code) doesn't prove the cause, say what you think and ask, or add
  *logging only*, before changing behaviour. Several speculative fixes had to be reverted.
- **Don't do things that weren't asked.** E.g. removing a card the user only mentioned in passing, adding a
  wiki-listed NPC (the Archive Guardian "Vine") that turned out not to be attackable. When the user's words
  are ambiguous, ask one short question first.
- **Understand what was meant before delegating.** An "A/B test" was misread as "old tier swords vs new
  icons" when the user meant "current icons vs in-game icons". Restate ambiguous requests in your own words
  if unsure.
- **Don't invent facts or timelines.** (A past session said "yesterday" when everything happened the same
  day.) Don't claim a feature works in-game — only the user can test in-game.
- **Report immediately when background work finishes**, and keep work moving; don't leave things idle.
- **Commit/push only buildable code**, so the user's `git pull` always runs. Run the build before every push.
- The user knows OSRS PvM deeply (tick-perfect play, boss mechanics). Treat their game knowledge as ground
  truth; ask them rather than guessing mechanics. They do not read code.
- The user dislikes reflexive agreement/apology ("it comes off as fake"). Acknowledge mistakes once, plainly,
  and fix them.
- When explaining how something works, explain the *signal the plugin uses* and *when it could be wrong*, so
  the user can catch bad assumptions before testing (they asked for this explicitly).

### Testing loop that works
1. You change code → build → commit → push.
2. User runs the "All in one" launch line, turns on `::pvmdebug` in game, plays.
3. User runs the cut script (section 5) → the debug log lands in the private repo `wagyugod/pvm-debug-logs`
   (local clone `C:\Users\12039\pvm-debug-logs`), the lost-tick video in `S:\RS Footage\Lost Ticks Analysis`.
4. You read the log (and, now that you're local, can watch the clips), find the tick the user names, explain.

With a local agent you can read `%USERPROFILE%\.runelite\plugin-data\pvm-performance-hub\debug\` and the
footage folder directly — that was the whole point of moving off the cloud session.

---

## 3. Shared conventions

- Both repos have `AGENTS.md` (RuneLite plugin rules + Jagex third-party restrictions). Follow it exactly:
  Java 11, no reflection/JNI/processes/dynamic code, `@Inject Gson`/`OkHttpClient`, all file I/O through
  `Filepath` (needs `internalName`), no disk/network I/O on the client thread, clean up in `shutDown()`,
  `log.debug` for diagnostics, **gameval constants** (`net.runelite.api.gameval.*ID`) instead of magic
  numbers, never rename a config `keyName`/group without a migration, optimise PNGs.
- **Boss-helper restrictions matter for the tracker's overlay:** it may only show the player's own running
  figures. No attack-ready cue/cooldown, no live idle-gap counter, no phase/target/dive state, nothing about
  what the boss does. (A reviewer might still object to the overlay's "(N lost)" suffix; it has its own
  switch.)
- Commit messages end with:
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01EzXMhawG3i4eCbLwVTSWzc
  ```
  (Use your own session's attribution if your harness gives one.) Don't put model names in code or PRs.
- After any change, tell the user *what to test in-game*; don't declare done until they confirm.
- Plugin Hub submission (when the user asks): fork `runelite/plugin-hub`, add `plugins/<name>` with
  `repository=https://github.com/wagyugod/<repo>.git` and `commit=<full hash>`, PR description ends with
  `Generated-by: Claude Code`. Repo must be public.

---

## 4. PvM Performance Hub

### 4.1 Core idea
A tick is **lost** only if all three hold: the weapon is off cooldown, a **valid target** exists, and the player
didn't attack a valid target. Attacking a valid target is "active" even if the engine thought the cooldown
hadn't elapsed. Consecutive lost ticks form a gap. Most per-boss work is defining "valid target" correctly.

### 4.2 Metrics (display names = exact user wording; `engine/Stat`)
- **Ticks Used** = attacks ÷ (attacks + lost ticks). Not ÷ fight length (the user once asked why 85.7% with
  24 lost in a 7:18 fight: 144 attacks / 168).
- **Ticks Used (3+)**, **Ticks Used (5+)** (5+ off by default in both settings and overlay): only lost ticks
  inside gaps of ≥ N count. The 10+ variant was removed entirely (old records still load).
- **Most Ticks Missed In A Row** (was "Longest Gap"; "In a row" in narrow spots).
- **Potted Hits**: at max boost for the attack's style (ranged +4+10%, melee super combat on Atk+Str, magic
  saturated/imbued heart if in inventory else magic potion); margin setting in levels.
- **Offensive Prayer**: Rigour/Deadeye/Eagle Eye; Augury/Mystic Vigour/Mystic Might; Piety/Chivalry/Zeal/
  Ultimate Strength.
- **Spec Attack Below 100%** (was Special Attack Utilization; inverted): % of ticks with spec energy below
  100%. Higher is better (energy never wasted at full).
- **Thrall Utilization**: % of ticks with a thrall up, Arceuus spellbook only.
- **Off-prayer Hits (beta)**: % of hits taken that the right protection prayer would have stopped; only for
  fights with a classifier (`fight/HitClassifiers`, `OffPrayerTracker`).
- Also Fight Length (graph y-axis in m:ss), Lost ticks count.

### 4.3 How attacks are detected (most fragile part — read carefully)
There is **no "you attacked" event** in RuneLite. Current signals, combined per tick (one attack max):
1. **Attack animation** on the local player (`onAnimationChanged`) that is on the **allowlist** in
   `fight/AttackAnimations` (301 ids: weapon attacks, specs, combat spells). Thrall casts, Vengeance, Death
   Charge, teleports, blocks, eating are not attacks. With `::pvmdebug`, other animations print
   "anim X ignored (not an attack animation)" — that's how missing real attacks get found.
2. **Replayed animation**: the client restarts an identical animation without an AnimationChanged event
   (2-tick tome attacks). Detected by the animation frame going backwards (`detectReplayedAttack`).
3. **Untargeted animation**: an attack animation that starts before `getInteracting()` is set is re-checked
   at tick processing.
4. **Special attack energy dropping** (`VarPlayerID.SA_ENERGY`, `onVarbitChanged`) = a spec on that tick,
   even with no animation (RuneLite's Special Attack Counter uses this). Confirmed working in a user log
   (tome spec right after swapping from Eye of Ayak plays animation -1).
5. **Zorya's tome effects** (`onGraphicChanged`, spotanims `VFX_ZORYA_TOME_*`) — as Attack Timer Metronome
   does.
6. **Tome stacks going up** (`VarPlayerID.ZORYAS_TOME_STACKS`) = the tome spec (stacks rise on the cast tick).

**Rejected (proven by logs, don't retry):** Hitpoints/skill XP (arrives when the hit *lands*, 1–2+ ticks late;
misses give none); hitsplats (thrall and Vengeance damage are type 16 "yours" like weapon hits); the player's
own projectiles (never reported with `getSourceActor()==player`, even with Detailed Combat Log's fallback);
ammo/charge counts (quiver, Ghommal's penny save ammo); sound effects (not sent when muted); a stack *drop*
as an attack (it invented an attack). XP, hitsplats, projectiles and weapon switches are still **logged** in
`::pvmdebug` for diagnosis.

Reference plugins studied (cloned under the old session's /tmp, re-clone if needed): Attack Timer Metronome
(`ngraves95/attacktimer` — the user uses it daily; its tome handling is wrong, see below), InfernoStats,
Detailed Combat Log (`AdancitoPR/runelite-combatlog`), Gauntlet Performance Tracker
(`powerus117/RLCGPerformanceTracker`), RuneLite core `specialcounter`.

### 4.4 Attack speed
- Item stats `aspeed` via `ItemManager`; rapid −1; autocast = 5; **harmonised nightmare staff** on standard
  spellbook = 4.
- **Powered staves** are magic regardless of style name: fixed list in `engine/PoweredStaves` (tridents,
  sang/holy sang, shadow, warped/accursed/Thammaron's sceptres, Eye of Ayak, Gauntlet staves, Dawnbringer).
  (The user rejected detection by style names: "classify it by weapon".)
- **Zorya's tome** (`fight/ZoryasTome`): any tome attack is **2 ticks if the game's stack count was > 0 going
  into it, else 3** — including a spec cast while buffed (two specs in a row → 3−1+3 = 5 stacks). Uses the
  game's own varp, never animations (spec animation can be missing or show up 3 ticks late on the next
  attack). Attack Timer Metronome counts stacks itself from animations + XP and drifts — that's why the user
  thought they couldn't attack when they could.
- Doom grub death resets the cooldown.

### 4.5 Eating
"Eat" menu click + that item's count dropping in the inventory within 2 ticks (`onItemContainerChanged`).
Not the eat animation (shared with drinking, often hidden during attack animations — a past bug). Eating is
**excused only at Vardorvis** (+3 ticks to the remaining cooldown); everywhere else eat delay counts as lost
by design. Combo-eat only applies one delay.

### 4.6 Valid targets
Shared layer `TargetRules.canBeTargeted`: not dead/dying (`NpcUtil.isDying`), health bar not empty, the
**transformed** composition has an "Attack" option, not in the fight's spawn/despawn/transition/death
animations, not recovering (per-NPC waits in `RespawnRecovery`). Then per-fight id rules. An attack counts
as on target if the NPC was valid this tick or the previous one (`isAttackOnTarget`). `TargetState` also has
**optional targets** (attacking them is on target, but they never make a tick available).

Per fight (user-confirmed rules; ✔ = verified in the user's logs):
- **Zulrah** ✔: only Zulrah; not while diving/under water/surfacing (`ZulrahDiveTracker`); 2-tick wait after
  re-surfacing (`SURFACE_IMMUNE_TICKS`) but **none after her first rise** (a spec landed then). The fight
  starts as soon as she can be hit, not on first attack. **Open:** at tick 228 of a log an Ayak hit landed
  during the "2-tick wait" → the wait may be too long; asked the user whether to cut it to 1 or 0, no answer.
- **Vardorvis** ✔: boss only; eating excused. Counts awakened kills.
- **Phosani's Nightmare** ✔ (most iterated):
  - Boss phases: boss + husks + parasites. While husks are in their spawn animation nothing counts (you
    can't attack her as they spawn).
  - Pillar phase: ready totems (+ husks, parasites); a pillar with a full charge bar (hp 100) is done.
  - Sleepwalker summon → "shield down" state: only sleepwalkers count; leftover parasites don't, until
    "The Nightmare restores her shield" (checked in any chat type) **or** she is attackable again / pillars
    appear (fallback, because the message was once missed).
  - Final phase (PHASE_05): she **and** sleepwalkers count (you can hit her to speed things up).
  - 1-tick wait on the tick she becomes attackable (new phase, by id/attackable/hp change), not after
    mid-phase teleports. 1-tick wait on the tick each sleepwalker appears. Dying sleepwalkers aren't
    targets. Surge/teleport ticks are genuine losses (user-confirmed). Spores are avoidable → count.
- **Doom of Mokhaiotl**: boss (all forms) + grubs; per-delve segments ("Delve N"); a run ends on Leave/death/
  logout.
- **Inferno / Fortis Colosseum**: region fights; any wave NPC counts; per-wave segments. Per-wave chat lines
  post as each wave ends.
- **The Fractured Archive** (new raid): each boss room is **its own fight** (start on first attack, end at
  "Challenge complete", own chat summary "The Fractured Archive - <Boss>"); the hallway isn't measured.
  `fight/ArchiveEncounter`: Archive Guardian + inert guardian (battery, charged target); Bork + Smorks;
  Ezekiel + animated swords; Moia + undead binders; Dreadnoth + jellies; Dagon'hai Elder (16643/44) + 2 monks
  (16645/46, 16647/48) + imps; Xarth + corpses; Ocellus (P1–P3) + portals (rift/siphon/augmenter) + void vine.
  **Never** the Guardian's vines (dodged) or Ocellus's sword. Ocellus starts only when he becomes attackable.
- **Added last, untested** (agent-built from gameval ids; see README "Testing in-game"): Vorkath (+ zombified
  spawn), Duke Sucellus, Maggot King (+ larvae; nothing counts once he's dying), Phantom Muspah (all forms incl.
  shield; never spikes), Scurrius (boss only), Mad Angel (boss only; unsure which form id is fightable),
  Gauntlet + Corrupted Gauntlet (Hunllef only, two FightTypes, one toggle), Leviathan, Whisperer (nothing while
  in the shadow realm — varbit `DT2_LASSAR_IN_SHADOW_REALM`, unconfirmed), Yama (Yama + Judge of Yama required,
  **voidflares optional**; the **Judge only makes ticks available within the player's weapon range**,
  `engine/AttackRange` — purging staff 10 tiles; the Emberlight 4t→3t-near-fireballs speedup is **not**
  implemented: no reliable signal found). Kill-message names for Maggot King, Mad Angel, Duke (awakened) are
  guesses. DT2 bosses count awakened kills too (user's choice).

Fight lifecycle (`FightLifecycle`): after a kill the same fight can't restart until it's no longer detected
(except Fractured Archive bosses); leaving the instance ends a fight at once; abandon timeouts 10 ticks for
boss fights, 100 for region fights / Doom mid-delve, 1000 between delves.

### 4.7 Settings (order: Chat messages, Live overlay, Overlay contents, Side Panel Metrics, Bosses, Side panel breakdowns, History)
- Chat: After-kill summary, Per-wave messages (as each wave ends), Per-delve messages. (Doom whole-run summary
  removed — too spammy.)
- Live overlay: Show live overlay (on), Always show overlay (on), Fade after kill 60 s (only matters when
  always-show is off).
- Overlay contents: one switch per row (separate from the side-panel metric switches).
- Side Panel Metrics: one switch per metric; potted margin.
- Bosses: one toggle per fight (all on).
- History: Save kill history, Kills shown in side panel (10), session length 6 h (logout rule kept).
- `ConfigMigration` handles renamed/removed keys; key names are stable even where display names changed.

### 4.8 Side panel
OSRS look (`ui/OsrsTheme`: stone #3e3529/#2b2620, RuneScape fonts with 1px shadow, orange #ff981f). Boss
selector with `< >` arrows plus a click-to-type search (`ui/NameSearch`: "Ph" → Phosani's Nightmare, list
pushes content down). Last-N table + graphs (last N kills, real kill numbers on x). Sessions, per-wave/
per-delve/per-boss breakdowns, "Delete saved history". No "PvM Performance" header.

### 4.9 Debug (`::pvmdebug`) and dev commands
`::pvmdebug` toggles per-tick lines in chat (white; lost-tick lines entirely red) **and** a file
`%USERPROFILE%\.runelite\plugin-data\pvm-performance-hub\debug\pvmdebug-<date_time>.txt`, every line
prefixed with wall-clock `HH:mm:ss.SSS` (for lining up with video). One file per RuneLite session (everything
until RuneLite closes). Lines include: fight started (+ "(already running)" when turned on mid-fight), attack
lines (anim, on/OFF target (npc id), speed, style, potted, prayer, tome stacks, targets), LOST with every valid
target (id, anim, hp, no-attack), "attack counted from special attack energy / tome effect", tome stack
changes with the player's animation/frame, weapon switches, ate, ignored animations, sleepwalker phase
start/end + shield message, attacks before a fight started (with the reason), Whisperer shadow realm, plus
logged-only hitpoints XP, hitsplats (type, amount, yours/others), and own projectiles.
Other dev commands: `::pvmsim <fight> [seed] [save]`, `::pvmclear`, `::pvmstate`.

### 4.10 Code map (`src/main/java/com/pvmperformance`)
- `PvmPerformancePlugin` — all RuneLite glue (events, attack detection, lifecycle, chat, debug, overlay view).
- `PvmPerformanceConfig`, `ConfigMigration`, `VisibleMetrics`.
- `engine/` (pure): `TickEngine`, `TickSample`, `FightMetrics`, `Segment`, `Stat`, `SummaryFormatter`,
  `BoostRules`, `PoweredStaves`, `AttackRange`, `FightSimulator`, `SimProfile`.
- `fight/`: `FightType`, `TargetRules`, `TargetState`, `NpcSnapshot`, `RespawnRecovery`, `ZulrahDiveTracker`,
  `ZoryasTome`, `AttackAnimations`, `ArchiveEncounter`, `KillMessageParser`, `FightLifecycle`,
  `OffPrayerTracker`, `HitClassifiers`, `IncomingHitClassifier`, `Segmentation`.
- `history/` (pure): `KillRecord`, `KillRecordCodec` (JSON Lines), `KillLog`, `KillHistory`, sessions,
  `Aggregates`. `storage/KillStore`: `plugin-data/pvm-performance-hub/<accountHash>/<fight>.jsonl`, append-only,
  executor thread only.
- `live/` (pure): `LiveSnapshot`, `LiveVisibility`, `LiveRows`, `LiveView`, `OverlayContents`.
- `ui/`: `PvmPanel`, `GraphPanel`, `CycleSelector`, `NameSearch`, `AxisFormat`, `ShadowLabel`, `OsrsToolTip`,
  `OsrsTheme`, `LiveOverlay`.
- `tools/cut-lost-ticks.ps1` + `.cmd` (section 5). `README.md` documents rules, settings, in-game checklist.

### 4.11 Open items (tracker)
1. Zulrah: is the 2-tick wait after re-surfacing too long (tick 228 hit landed)? Ask the user.
2. Phosani pillar phase: when the last pillar is finished by a spell still in flight, the next tick counts as
   lost (bar reads 93% until the hit lands). Proposed fix (refund lost ticks on the last pillar since your last
   cast once it fills) — awaiting the user's OK.
3. Verify the 10 new bosses in game (kill messages, Whisperer varbit, Yama Judge range/phase, Mad Angel form).
4. Emberlight 3t near Yama's fireballs: no signal found; revisit with footage/logs.
5. Attack detection: other weapons may have hidden-animation cases like the tome; find them via "ignored"
   lines and footage, fix case by case with a proven signal.
6. Plugin Hub: never submitted; check the overlay "(N lost)" risk; the Fractured Archive and 10 new bosses
   need in-game confirmation first.

---

## 5. Tools and data

- **Debug logs repo:** `wagyugod/pvm-debug-logs` (private). Local clone `C:\Users\12039\pvm-debug-logs`.
  Files are named like the cut video (`<Boss>_<MM-DD-YY>_<N> Lost Ticks.txt`) and contain the **whole** debug
  session (all kills since RuneLite started); find the kill by "fight started" / tick numbers.
- **Cut script:** `tools/cut-lost-ticks.ps1` (+ `cut-lost-ticks.cmd` double-click launcher, which `git pull`s
  first; the user has a desktop shortcut to the .cmd). Needs ffmpeg (`winget install Gyan.FFmpeg`). With no
  arguments: newest video under `S:\RS Footage` (searches subfolders — the NVIDIA app saves into per-app folders
  like `Desktop`), newest debug log; output `S:\RS Footage\Lost Ticks Analysis\<Boss>_<MM-DD-YY>_<N> Lost
  Ticks.mp4` (" (2)" if it exists) + a copy of the log next to it, then pushes the log to `pvm-debug-logs`.
  Each clip starts 2 s before a lost tick, consecutive lost ticks play as one clip, overlapping clips merge.
  Recording start time comes from the file name (OBS `2026-11-05 14-32-07`, NVIDIA `Desktop 2026.11.05 -
  18.03.31.121`), else file creation time; `-SyncTick N -SyncAt "m:ss.s"` pins it exactly. Never tested on
  PowerShell by the author (no pwsh in the cloud) — the user has run it successfully.
- **Combat Masteries offline renderer** and the OSRS cache/model tools (helmet icon renders, window
  comparisons against Combat Achievements screenshots) lived only in the cloud session's scratch space and
  are gone. Rebuild if needed: the RuneLite client source + an OSRS flat cache let you render widgets and item
  models offline.

---

## 6. Combat Masteries

### 6.1 What exists
- **Tasks** (`src/main/resources/com/combatmasteries/tasks.json`): 1 point each, Speed or Challenge, content
  e.g. Inferno, Colosseum, ToB/HM ToB, CoX CM, ToA (expert), Doom of Mokhaiotl, Awakened bosses, Yama
  Contracts. Conditions: `UNDER_TIME`, `TEAM_SIZE`, `MIN_RAID_LEVEL`, `MIN_DELVE_LEVEL`, `INFERNO_NO_PILLARS`,
  one-inventory `trip`. Never change a task `id` after release. Deep Delver descriptions read "Defeat the Doom
  of Mokhaiotl at delve level X". Times were set by the user (e.g. CM solo 30/28/26/25/24, trio 24/22/20/19/18,
  5-man 22/20/19/18/17; ToB 3s 16/15/14:30/14/13:30, 5s 13:30/13/12:30/12, duo 24/22/21/20/19; no 4-man ToB).
- **Tiers**: Ascended 25%, Exalted 50%, Immortal 90% of points (rounded down).
- **Helmet cosmetic** (`cosmetic/`): client-side recolour of the Tzkal Slayer Helmet's eyes, a slow 12 s shimmer
  across the slit. Ascended red→orange and spills light onto the metal around the eyes; Exalted ice blue→azure
  and Immortal Radiant-oathplate white glow in the slit only (spill was tried and reverted — looked two-toned).
  Reward text for all three says "light spills onto the metal around the eyes. Eyes glow …" (user kept it).
- **Tracking** (`tracking/`): times from completion messages (needs Precise timing), team size, ToA raid level
  peak, Doom deepest level varbit, Inferno pillars at wave 4, one-inventory trips, existing PB credit via Chat
  Commands config. Only normal worlds.
- **In-game window** (`ui/window/`): a pixel-faithful copy of the Combat Achievements window (Overview, Tasks,
  Bosses, Rewards tabs), drawn with game sprites/fonts. Built in a builder + critic loop whose only criterion
  was the user's: "Does the UI presented to me identically match the corresponding section in the combat
  achievement reference screenshot?" Details:
  - Task rows use the game's own item sprites as content icons (`ItemManager.getImage`, `ui/window/ItemIcons`,
    `ContentIcons`): Infernal cape, Dizana's quiver, Scythe / holy scythe, Twisted bow, Tumeken's shadow,
    Avernic treads, Radiant oathplate helm, Sanguine torva helm (Awakened). The user chose in-game icons over
    hand-drawn ones.
  - Small tier icons from the 3/4-view helmet renders.
  - Overview "Mastery Tiers" boxes are clickable → Tasks tab, filters reset.
  - Bosses tab cards (alphabetical ignoring "The"), incl. a 0/0 The Fractured Archive card on purpose (user:
    new raid, keep it).
  - Rewards: one line per tier.
  - **Screenshot button** (Overview only, title bar left of the close X, stone box with RuneLite's camera
    icon) saves just the window via `ImageCapture` to `screenshots/<player>/Combat Masteries/`.
- **"Masteries" button on the real Combat Achievements window** (`ui/CombatAchievementsButton`): a stone
  9-slice button like WikiSync/Temple on the collection log, 71×22, label "Masteries", re-added after each CA
  tab build script, opens the matching tab.
- **Plugin icon**: 16×16 sidebar `icon.png` = option "D" (whole helmet at its inventory angle, uncropped);
  48×48 hub `icon.png` at the repo root (PR #2).
- Settings (`CombatMasteriesConfig`): chat messages, completion pop-up, desktop notification, credit existing
  PBs, glowing eyes, tier to display.
- Dev commands: `::cmtier`, `::cmkill`, `::cmdelve`, `::cmdoom`, `::cmtoa`, `::cmhelm`, `::cmtrip`,
  `::cmattempt`, `::cmreset` (README).

### 6.2 Open items (Combat Masteries)
1. Merge PR #2 (hub icon + properties) before submitting.
2. User's final review, then Plugin Hub submission (section 3).
3. In-game checks still outstanding: the Masteries button on all four CA tabs, tier boxes, screenshot button
   (file lands, notification, image excludes the button), item-sprite icons loading, helmet glow on/off GPU and
   117 HD, Doom "deepest" meaning reached vs cleared.

---

## 7. Where to find more
- `README.md` in each repo: full rules, settings and in-game checklists.
- `git log` on `claude/kind-brahmagupta-fspgga`: commit messages explain each rule and the log evidence behind it.
- PR descriptions: tracker #1 (stale in places — the code is newer), masteries #1/#2.
