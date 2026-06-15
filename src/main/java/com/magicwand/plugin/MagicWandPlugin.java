package com.magicwand.plugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

public class MagicWandPlugin extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register command
        if (getCommand("givewand") != null) {
            getCommand("givewand").setExecutor(this);
        }
        
        getLogger().info("MagicWandPlugin enabled! Use /givewand to get the magic item.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicWandPlugin disabled.");
    }

    // ==================== COMMAND ====================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("givewand")) {
            return false;
        }

        if (!sender.hasPermission("magicwand.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is offline.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Console must specify a player: /givewand <player>");
            return true;
        }

        ItemStack wand = createMagicWand();
        target.getInventory().addItem(wand);

        String msg = ChatColor.LIGHT_PURPLE + "You have been given the " + ChatColor.BOLD + "Arcane Wand" + ChatColor.RESET + ChatColor.LIGHT_PURPLE + "!";
        target.sendMessage(msg);

        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Gave Arcane Wand to " + target.getName() + ".");
        }

        return true;
    }

    private ItemStack createMagicWand() {
        ItemStack wand = new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Arcane Wand");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Infused with ancient evocation magic.",
                    ChatColor.DARK_PURPLE + "Right-click to summon a devastating line of evoker fangs!"
            ));
            // This matches your item: warped_fungus_on_a_stick[minecraft:item_model="template:wand"]
            meta.setItemModel(NamespacedKey.fromString("template:wand"));
            
            wand.setItemMeta(meta);
        }
        return wand;
    }

    // ==================== EVENT LISTENER ====================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel()) return;

        NamespacedKey model = meta.getItemModel();

        // === MAGIC BOOK (Offhand only) ===
        if (hand == EquipmentSlot.OFF_HAND && item.getType() == Material.CARROT_ON_A_STICK) {
            NamespacedKey bookModel = NamespacedKey.fromString("template:magic_book");
            if (model.equals(bookModel)) {
                if (player.hasCooldown(Material.CARROT_ON_A_STICK)) return;

                player.setCooldown(Material.CARROT_ON_A_STICK, 100); // 5 second cooldown
                event.setCancelled(true);
                castMagicBook(player);
            }
            return;
        }

        // === WARPED FUNGUS WAND (Main hand) ===
        if (hand == EquipmentSlot.HAND && item.getType() == Material.WARPED_FUNGUS_ON_A_STICK) {
            NamespacedKey wandModel = NamespacedKey.fromString("template:wand");
            if (model.equals(wandModel)) {
                if (player.hasCooldown(Material.WARPED_FUNGUS_ON_A_STICK)) return;

                player.setCooldown(Material.WARPED_FUNGUS_ON_A_STICK, 40);
                showCooldownActionBar(player, 40);
                event.setCancelled(true);
                castEvokerFangLine(player);
            }
        }
    }

    private void castEvokerFangLine(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();

        // Horizontal direction for nice ground-level line of fangs
        Vector direction = eye.getDirection().clone();
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(0, 0, 1); // fallback if looking straight up/down
        }
        direction.normalize();

        Location base = player.getLocation().clone().add(0, 0.4, 0);

        // ========== CASTING EFFECTS ==========
        // Big magic burst at caster
        world.spawnParticle(Particle.WITCH, eye, 120, 1.2, 1.2, 1.2, 0.03);
        world.spawnParticle(Particle.END_ROD, eye, 40, 0.9, 0.9, 0.9, 0.08);
        // DRAGON_BREATH removed temporarily to fix crash on this Paper version

        // Sound - louder volumes
        player.playSound(eye, Sound.ENTITY_EVOKER_CAST_SPELL, 2.0f, 1.0f);
        player.playSound(eye, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.5f, 1.2f);

        // Satisfying magic "ding" / chime (much louder)
        player.playSound(eye, Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 1.8f);
        player.playSound(eye, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.8f, 1.6f);
        player.playSound(eye, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1.5f);

        // ========== PARTICLE LINE ==========
        for (double d = 0.7; d < 11; d += 0.55) {
            Location particleLoc = base.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.WITCH, particleLoc, 8, 0.18, 0.35, 0.18, 0.015);
            world.spawnParticle(Particle.END_ROD, particleLoc, 3, 0.12, 0.25, 0.12, 0.03);
        }

        // ========== LINE OF EVOKER FANGS (simple & reliable) ==========
        int numFangs = 8;
        double spacing = 1.3;
        double startDistance = 2.0;                    // start a bit further in front
        double playerY = player.getLocation().getY() + 0.3;   // same level as player is standing

        for (int i = 0; i < numFangs; i++) {
            double distance = startDistance + (i * spacing);
            Vector offset = direction.clone().multiply(distance);
            double targetX = base.getX() + offset.getX();
            double targetZ = base.getZ() + offset.getZ();

            Location fangLoc = new Location(world, targetX, playerY, targetZ);

            EvokerFangs fang = world.spawn(fangLoc, EvokerFangs.class);
            fang.setOwner(player);
            // Stagger the attack delay so fangs snap shut in a wave
            fang.setAttackDelay(i * 2);
        }

        // Final burst at the end of the fang line
        double endDistance = startDistance + ((numFangs - 1) * spacing) + 1.5;
        Location endPoint = base.clone().add(direction.clone().multiply(endDistance));
        world.spawnParticle(Particle.EXPLOSION_EMITTER, endPoint, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.DRAGON_BREATH, endPoint, 35, 0.7, 0.7, 0.7, 0.06);
        world.spawnParticle(Particle.WITCH, endPoint, 50, 0.8, 0.8, 0.8, 0.02);
    }

    // ==================== COOLDOWN ACTION BAR ====================
    private void showCooldownActionBar(Player player, int totalTicks) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticksLeft = totalTicks;

            @Override
            public void run() {
                if (!player.isOnline() || !player.hasCooldown(Material.WARPED_FUNGUS_ON_A_STICK)) {
                    player.sendActionBar(Component.text(""));
                    this.cancel();
                    return;
                }

                ticksLeft = player.getCooldown(Material.WARPED_FUNGUS_ON_A_STICK);
                double seconds = ticksLeft / 20.0;

                Component message = Component.text("Cooldown: ", NamedTextColor.GRAY)
                        .append(Component.text(String.format("%.1fs", seconds), NamedTextColor.RED));

                player.sendActionBar(message);
            }
        }.runTaskTimer(this, 0L, 2L); // Update every 2 ticks (0.1s)
    }

    // ==================== HELIX PARTICLE EFFECT ====================
    private void spawnMagicHelix(Location center, double radius, double height, double density) {
        World world = center.getWorld();

        for (double y = 0; y < height; y += density) {
            double angle = y * 3.2; // Balanced for clear spiral look

            // First helix (WITCH particles)
            double x1 = Math.cos(angle) * radius;
            double z1 = Math.sin(angle) * radius;
            Location loc1 = center.clone().add(x1, y, z1);
            world.spawnParticle(Particle.WITCH, loc1, 3, 0.04, 0.04, 0.04, 0.01);

            // Second helix (END_ROD particles) - offset by 180 degrees
            double x2 = Math.cos(angle + Math.PI) * radius;
            double z2 = Math.sin(angle + Math.PI) * radius;
            Location loc2 = center.clone().add(x2, y, z2);
            world.spawnParticle(Particle.END_ROD, loc2, 2, 0.03, 0.03, 0.03, 0.005);
        }
    }

    // ==================== MAGIC BOOK (Offhand) ====================
    private void castMagicBook(Player player) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        // === FULL HELIX EFFECT ===
        spawnMagicHelix(loc, 1.7, 4.0, 0.20);

        // Sounds
        player.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.1f);
        player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.3f);
        player.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.4f);

        // Apply buffs
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 300, 1, true, true)); // Speed II - 15 seconds

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, 240, 0, true, true)); // Strength I - 12 seconds

        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.REGENERATION, 200, 0, true, true)); // Regeneration I - 10 seconds

        // Message
        player.sendMessage(Component.text("You feel a surge of magical energy!", NamedTextColor.LIGHT_PURPLE));
    }
}
