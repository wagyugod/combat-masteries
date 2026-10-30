# Combat Masteries

A RuneLite plugin that adds an achievement system beyond Grandmaster Combat Achievements.

- **Tasks** are extremely hard boss challenges. Each is worth 1 point.
- **Tiers** unlock at a share of all tasks completed:

  | Tier | Needs | Tzkal slayer helmet eyes |
  |---|---|---|
  | Ascended Grandmaster | 25% of tasks | Glowing red |
  | Exalted Grandmaster | 50% of tasks | Glowing dark blue |
  | Immortal Grandmaster | 100% of tasks | Glowing white (Radiant oathplate's white) |

- **Reward**: the eyes of your Tzkal slayer helmet glow in your tier's colour. This is
  client-side only, so only you see it, and it only shows while you wear the helmet.

![Tzkal slayer helmet eye glow for each tier](docs/helmet-tiers.png)

*Offline render of the helmet's real model (42733) from the game cache. It uses the client's
lighting and RuneLite's colour conversion, so in-game results should be close but not identical.*

> **Prototype status:** the tasks in `tasks.json` are placeholders for testing. The real
> task list replaces them.

## Testing in-game

You need a JDK (11 or newer) and Git.

```bash
git clone <this repository>
cd combat-masteries
./gradlew run
```

This starts a RuneLite development client with the plugin loaded. To log in with a Jagex
account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
once first.

### Developer commands

The dev client runs in developer mode, which enables these chat commands. They do nothing in
a normal RuneLite install.

| Command | What it does |
|---|---|
| `::cmtier ascended` / `exalted` / `immortal` / `off` | Preview a tier's eyes without unlocking it |
| `::cmkill Boss_Name m:ss [dmg] [prayer] [food] [potion]` | Pretend you killed a boss, to test task completion. Use `_` for spaces, e.g. `::cmkill Zulrah 0:44.40` |
| `::cmattempt` | Show what the current attempt has recorded |
| `::cmreset` | Wipe this account's mastery progress |

### What to check

1. **Helmet**: wear a Tzkal slayer helmet and run `::cmtier ascended`, then `exalted`, then
   `immortal`. The eye slit should change colour and glow, the halo should hide when you face
   away, and `::cmtier off` should restore the stock eyes. Please try it with the GPU plugin
   on and off, and with 117 HD if you use it.
2. **Side panel**: the helmet icon in the sidebar should list the tasks and show progress.
3. **Task completion and unlocking**: `::cmkill Zulrah 0:50 dmg` should complete nothing
   (too slow for one task, took damage for the other). `::cmkill Zulrah 0:44.40` should then
   complete both Zulrah tasks. That's 2 of the 8 sample tasks, so it also unlocks Ascended:
   check for the chat messages, notifications, the panel update and the red eyes.
   `::cmreset` starts over.
4. **Real kills**: kill any boss in `tasks.json` and check `::cmattempt` during the fight. A
   real kill also records your in-game personal best for that boss.

## How tracking works

- **Kills and times** come from the game's own kill count and duration chat messages. Turn
  on *Settings → Precise timing* in-game: without it, times are rounded to whole seconds.
- **An attempt** starts when you attack the boss or it attacks you. For wave content (Inferno,
  Fight Caves) it starts when you enter the region. During an attempt the plugin notes damage
  taken (hitsplats on you), any active prayer, and Eat and Drink clicks. It ends at the kill,
  or fails on death, logout, hop or leaving the region.
- **Existing personal bests** credit speed-only tasks, so you don't have to repeat a time you
  have already beaten. They come from kill messages ("Personal best: 0:41.40") and from
  RuneLite's Chat Commands plugin, which also reads your POH adventure log. Tasks with
  restrictions need a kill the plugin watched.
- Progress only counts on normal worlds: not Leagues, Deadman, beta, speedrunning or
  tournament worlds.
- Everything is stored locally in your RuneLite profile. Nothing is sent anywhere.

## Writing tasks

Tasks live in `src/main/resources/com/combatmasteries/tasks.json`:

```json
{
  "bosses": [
    { "name": "Vorkath", "npcs": ["Vorkath"] },
    { "name": "TzKal-Zuk", "regions": [9043] }
  ],
  "tasks": [
    {
      "id": "vorkath_flawless_streak",
      "name": "Dragonbane Discipline",
      "description": "Kill Vorkath 10 times in a row without taking any damage.",
      "boss": "Vorkath",
      "conditions": [{ "type": "NO_DAMAGE_TAKEN" }],
      "consecutive": 10
    }
  ]
}
```

- `boss` is the name exactly as the kill count message prints it ("Your **TzKal-Zuk** kill
  count is...").
- `npcs` are the NPC names that start an attempt. `regions` make the whole stay in a region one
  attempt, for wave content.
- `conditions` must all hold for one kill. The available types are `MAX_TIME` (with `seconds`),
  `NO_DAMAGE_TAKEN`, `NO_PRAYER`, `NO_FOOD` and `NO_POTIONS`.
- `consecutive` (default 1) needs that many qualifying kills in a row. Any non-qualifying kill
  or death at that boss resets it.
- Never change an `id` after release: progress is saved against it.

## Credits

The kill message patterns are adapted from RuneLite's Chat Commands plugin (BSD-2-Clause).
