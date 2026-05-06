# Action Library Roadmap

## Goal

- Make common scripting scenarios configurable without writing expressions in most cases.
- Keep advanced composition available through capture + expression as a fallback.
- Prefer unifying models over adding one-off actions.

## Execution Order

1. P0: unify entity query parameters across condition, wait, capture, follow, hunt, and trigger flows.
2. P0: fill symmetry gaps between condition, wait, capture, and trigger domains.
3. P0: add stable control-flow targets such as labels.
4. P0: ship high-frequency templates for patrol, occupancy, boss, and relog flows.
5. P1: add structured branching and loop primitives.
6. P1/P2: add advanced GUI, screen, packet, and debugging capabilities.

## P0

### Entity Query Unification

- [x] Define a shared entity filter model:
  - `entityType`
  - `entityName`
  - `matchMode`
  - `radius`
  - `upRange`
  - `downRange`
  - `minCount`
  - `maxCount`
  - `ignoreSelf`
  - `ignoreInvisible`
  - `sortMode`
- [x] Apply the shared model to:
  - `condition_entity_nearby`
  - `wait_until_entity_nearby`
  - `capture_nearby_entity`
  - `capture_entity_list`
  - `follow_entity`
  - `hunt`
  - nearby-entity triggers
- [x] Make condition, wait, and capture summaries show the same core filter information.
- [x] Keep old configs compatible by defaulting missing new params.

### Condition / Wait / Trigger Symmetry

- [x] Add `condition_player_list`
- [x] Add `wait_until_player_list`
- [x] Add `condition_scoreboard`
- [x] Add `wait_until_scoreboard`
- [x] Add `condition_packet_text`
- [x] Add `condition_bossbar`
- [x] Review all existing `condition_*` and `wait_*` actions for parameter symmetry.

### Control Flow Stability

- [x] Add `label`
- [x] Add `goto_label`
- [x] Keep `goto_action` for compatibility, but stop recommending it for new scripts.

### High-Frequency Templates

- [x] Point occupied -> skip to next point
- [x] Boss patrol across multiple points
- [x] Player detected -> change line / retreat
- [x] Target missing after teleport -> retry / next point
- [x] Wait for respawn -> kill once -> continue
- [x] Safe menu reopen / GUI recovery

## P1

### Structured Flow

- [x] Add `if_else`
- [x] Add `switch_var`
- [x] Add `branch_table`
- [x] Add `for_each_point`
- [x] Add `for_each_list`
- [x] Add `while_condition`
- [x] Add retry block primitives

### Better Capture Outputs

- [x] Extend entity list capture with ready-to-use aggregate variables:
  - `_player_count`
  - `_hostile_count`
  - `_passive_count`
  - `_nearest_player_name`
  - `_nearest_player_distance`
  - `_nearest_hostile_name`
  - `_nearest_hostile_distance`
- [x] Add more direct point-state helper templates before adding dedicated one-off actions.

## P2

### GUI / Screen / Packet Advanced Actions

- [x] Add `condition_gui_element`
- [x] Add `wait_until_gui_element`
- [x] Add `condition_screen_region`
- [x] Add `condition_packet_field`
- [x] Add `wait_until_packet_field`

### Debugging

- [x] Add print variable action
- [x] Add print nearby entity summary action
- [x] Add print GUI summary action
- [x] Surface richer failure reasons in action debug output

## Current Sprint

- [x] Create this roadmap file
- [x] Unify `entityType` support across condition, wait, and capture nearby-entity actions
- [x] Add `minCount` support to nearby-entity condition and wait actions
- [x] Reflect new params in editor UI, runtime evaluation, summaries, and validation
- [x] Extend nearby-entity trigger with `entityType` support
- [x] Fix nearby-entity trigger change detection to include count/category changes
- [x] Compile and verify compatibility

## Next Sprint

- [x] Continue entity filter unification into `follow_entity`
- [x] Continue entity filter unification into `hunt`
- [x] Add `condition_player_list`
- [x] Add `wait_until_player_list`
- [x] Start `label` / `goto_label` design and compatibility plan
