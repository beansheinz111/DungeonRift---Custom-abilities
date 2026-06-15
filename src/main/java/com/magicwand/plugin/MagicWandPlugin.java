package com.magicwand.plugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MagicWandPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> windSwordUsers = new HashSet<>();

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
            sender.sendMessage(ChatColor.RED + "Console must specify a player.");
            return true;
        }

        ItemStack item;
        String itemName;
        String giveMessage;

        switch (label.toLowerCase()) {
            case "givewand":
                item = createMagicWand();
                itemName = "Arcane Wand";
                giveMessage = "Arcane Wand";
                break;
            case "givemagicbook":
                item = createMagicBook();
                itemName = "Tome of Arcane Surge";
                giveMessage = "Magic Book";
                break;
            case "givewindsword":
                item = createWindSword();
                itemName = "Zephyr Blade";
                giveMessage = "Wind Sword";
                break;
            default:
                return false;
        }

        target.getInventory().addItem(item);

        String msg = ChatColor.LIGHT_PURPLE + "You have been given the " + ChatColor.BOLD + itemName + ChatColor.RESET + ChatColor.LIGHT_PURPLE + "!";
        target.sendMessage(msg);

        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Gave " + giveMessage + " to " + target.getName() + ".");
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

    private ItemStack createMagicBook() {
        ItemStack book = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Tome of Arcane Surge");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "A mystical tome pulsing with raw magical energy.",
                    ChatColor.DARK_PURPLE + "Hold in offhand and right-click to gain",
                    ChatColor.DARK_PURPLE + "temporary Speed, Strength, and Regeneration."
            ));
            meta.setItemModel(NamespacedKey.fromString("template:magic_book"));
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createWindSword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Zephyr Blade");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "A blade infused with the power of the wind.",
                    ChatColor.DARK_AQUA + "Strike enemies to launch yourself skyward",
                    ChatColor.DARK_AQUA + "with cloud particles and wind bursts.",
                    ChatColor.GRAY + "Fall damage is nullified after use."
            ));
            meta.setItemModel(NamespacedKey.fromString("template:wind_sword"));
            sword.setItemMeta(meta);
        }
        return sword;
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

                player.setCooldown(Material.CARROT_ON_A_STICK, 600); // 30 second cooldown
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

    // ==================== WIND SWORD (Hit Entity) ====================
    @EventHandler
    public void onEntityHitWithWindSword(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.DIAMOND_SWORD || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel()) return;

        NamespacedKey model = meta.getItemModel();
        if (!model.equals(NamespacedKey.fromString("template:wind_sword"))) return;

        // Prevent spam
        if (player.hasCooldown(Material.DIAMOND_SWORD)) return;
        player.setCooldown(Material.DIAMOND_SWORD, 60); // 3 second cooldown

        // Elevate the player (strong enough for mace smash)
        player.setVelocity(player.getVelocity().setY(1.6));

        Location loc = player.getLocation();
        World world = player.getWorld();

        // Cloud particles
        for (int i = 0; i < 40; i++) {
            double x = (Math.random() - 0.5) * 2.5;
            double y = Math.random() * 2;
            double z = (Math.random() - 0.5) * 2.5;
            world.spawnParticle(Particle.CLOUD, loc.clone().add(x, y, z), 1, 0.1, 0.1, 0.1, 0.02);
        }

        // Sounds (wind burst)
        player.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.2f);
        player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        // Track player for fall damage protection
        windSwordUsers.add(player.getUniqueId());

        player.sendMessage(Component.text("Wind burst!", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        if (windSwordUsers.remove(player.getUniqueId())) {
            event.setCancelled(true); // No fall damage
            // Play landing sound to "stop" wind effect
            player.playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 0.8f, 1.2f);
        }
    }

    // ==================== SKULL CRUSHER MACE ====================
    @EventHandler
    public void onMaceSmash(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.MACE || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel()) return;

        if (!meta.getItemModel().equals(NamespacedKey.fromString("skull_crusher"))) return;

        // Only trigger on falling attacks (mace smash style)
        if (player.getFallDistance() < 0.5) return;

        Location center = player.getLocation();
        World world = player.getWorld();

        // Simple dark particle burst
        world.spawnParticle(Particle.SMOKE, center, 150, 4, 2, 4, 0.05);
        world.spawnParticle(Particle.SQUID_INK, center, 80, 3, 1.5, 3, 0.03);

        // Wither particles + sound
        world.spawnParticle(Particle.WITHER, center, 80, 3, 1, 3, 0.1);
        player.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
        player.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);

        // Apply Wither II to nearby entities
        double radius = 5.0;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                if (living != player) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1)); // Wither II for 5 seconds
                }
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
