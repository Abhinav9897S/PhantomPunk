# Controls

## Keyboard

| Action | Primary | Alternative | Notes |
| --- | --- | --- | --- |
| Move left / right | A / D | Left / Right arrows | Acceleration and friction; full air control |
| Jump | Space | Z | Hold for a higher jump, tap for a short hop. Works 0.1 s after leaving a ledge (coyote time) and if pressed up to 0.12 s before landing (buffer) |
| Strike | J | X | Forward by default |
| Strike upwards | hold W + J | Up + X | |
| Strike downwards (pogo) | hold S + J in the air | Down + X | Bounces you off enemies and spikes |
| Dash (Umbral Dash) | K | C, Left Shift | Unlocked by defeating the Gnarlwarden. One dash per jump in the air |
| Wall cling / wall jump (Moss Grip) | hold towards a wall while falling, then Jump | | Unlocked at the Drowned Chapel shrine. Climb a shaft by jumping between its two walls |
| Double jump (Wisp Leap) | Jump again in mid-air | | Unlocked by defeating the Gilded Warden |
| Light Bolt | L | Q | Costs one third of the soul orb. Unlocked at the Root Shrine |
| Focus (heal) | hold F | hold V | Stand still on the ground; costs one third of the soul orb per flame |
| Interact (rest, talk, trade, take) | W | Up, E | When a prompt is shown above an object |
| Look up / down | hold W or S while standing still | | The camera pans after a moment |
| Drop through a one-way ledge | S + Jump | | |
| Map | Tab | M | |
| Pause | Esc | P | |

## Menus

| Action | Keys |
| --- | --- |
| Move selection | W / S or Up / Down |
| Confirm | Enter, Space or J |
| Adjust a setting | A / D or Left / Right |
| Back | Esc or Backspace |

## Gamepad (standard SDL mapping, via gdx-controllers)

| Action | Button |
| --- | --- |
| Move / aim | Left stick or D-pad |
| Jump, confirm | A (south) |
| Strike | X (west) |
| Dash | Right bumper or B (east) |
| Light Bolt | Y (north) |
| Focus | Left bumper |
| Map | Back / Select |
| Pause | Start |

The gamepad code path is compiled and the controller backend starts correctly, but it was
**not tested with a physical controller** in the build environment (see `TEST_REPORT.md`).
If no controller backend is available, the game silently falls back to the keyboard.
