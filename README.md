# Magic Wand Plugin for Minecraft 1.21+

This plugin adds magic to `warped_fungus_on_a_stick[minecraft:item_model="template/wand"]`.

Right-clicking with the wand:
- Plays satisfying magic ding/chime sounds
- Spawns a line of particles
- Summons 8 evoker fangs in a terrain-following wave in front of the player

## How to Build & Install

### Requirements
- Java 17 or newer
- Maven (https://maven.apache.org/install.html)

### Steps

1. **Unzip** the `MagicWandPlugin-1.0.zip` file you downloaded.

2. You will see a folder called `MagicWandPlugin/`.

3. Open a terminal / command prompt and navigate into that folder:
   ```bash
   cd MagicWandPlugin
   ```

4. Build the plugin with Maven:
   ```bash
   mvn clean package
   ```

5. After it finishes, the compiled plugin jar will be here:
   `target/magicwand-plugin-1.0-SNAPSHOT.jar`

6. Copy that `.jar` file into your server's `plugins/` folder.

7. Restart your server (Paper recommended).

## How to Get the Wand

- In-game command: `/givewand`
- Or give it to someone else: `/givewand PlayerName`
- Or use the exact item:  
  `/give @s warped_fungus_on_a_stick[minecraft:item_model="template/wand"]`

## Resource Pack (for visuals)

A separate resource pack is included (`MagicWandResourcePack.zip`).

1. Unzip it.
2. Put the `MagicWandResourcePack` folder into your Minecraft client's `resourcepacks/` folder.
3. Enable it in **Options → Resource Packs**.
4. The wand will visually look like a glowing blaze rod (you can customize it later with your own model).

## Notes
- The plugin works even without the resource pack (behavior only).
- Make sure you are using Minecraft 1.21 or newer.
- The item uses the modern `item_model` component.

Enjoy your magic wand! 🪄
