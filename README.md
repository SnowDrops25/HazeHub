# HazeHub
Paper 26.3 / Java 25 lobby compass selector for HazeNetwork.

- Automatically gives a compass in the fifth hotbar slot (index 4).
- Right-click runs `/servers` (DeluxeMenus).
- Prevents dropping or moving the selector.
- Does not overwrite an occupied fifth slot if inventory is full.

## Build
`mvn clean package` with JDK 25 and internet access to the Paper Maven repository.
Result: `target/HazeHub.jar`.
Alternatively upload this source as a GitHub repository and use Actions > Build HazeHub > Run workflow. Download the artifact and unzip it to obtain `HazeHub.jar`.

## Install
Remove incompatible ItemJoin.jar, stop Hub-01, upload HazeHub.jar to plugins/, start server. DeluxeMenus `/servers` must already work.

Only install on the Hub-01 backend. The plugin has not been runtime-tested on Paper 26.3.
