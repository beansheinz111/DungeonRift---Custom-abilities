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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

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
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.WARPED_FUNGUS_ON_A_STICK) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasItemModel()) {
            return;
        }

        NamespacedKey model = meta.getItemModel();
        NamespacedKey expectedModel = NamespacedKey.fromString("template:wand");
        if (model == null || !model.equals(expectedModel)) {
            return;
        }

        // Prevent spamming
        if (player.hasCooldown(Material.WARPED_FUNGUS_ON_A_STICK)) {
            return;
        }
        player.setCooldown(Material.WARPED_FUNGUS_ON_A_STICK, 40); // 2 second cooldown

        event.setCancelled(true);

        // Cast the magic!
        castEvokerFangLine(player);
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
        world.spawnParticle(Particle.DRAGON_BREATH, eye, 25, 0.6, 0.6, 0.6, 0.04);

        // Sound
        player.playSound(eye, Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.95f);
        player.playSound(eye, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 0.8f, 1.3f);

        // Satisfying magic "ding" / chime (layered for nice feel)
        player.playSound(eye, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.9f);
        player.playSound(eye, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.7f);
        player.playSound(eye, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);

        // ========== PARTICLE LINE ==========
        for (double d = 0.7; d < 11; d += 0.55) {
            Location particleLoc = base.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.WITCH, particleLoc, 8, 0.18, 0.35, 0.18, 0.015);
            world.spawnParticle(Particle.END_ROD, particleLoc, 3, 0.12, 0.25, 0.12, 0.03);
        }

        // ========== LINE OF EVOKER FANGS (simple & reliable) ==========
        int numFangs = 8;
        double spacing = 1.25;
        double playerY = player.getLocation().getY() + 0.6;   // spawn at player's feet level

        for (int i = 1; i <= numFangs; i++) {
            Vector offset = direction.clone().multiply(i * spacing);
            double targetX = base.getX() + offset.getX();
            double targetZ = base.getZ() + offset.getZ();

            Location fangLoc = new Location(world, targetX, playerY, targetZ);

            EvokerFangs fang = world.spawn(fangLoc, EvokerFangs.class);
            fang.setOwner(player);
            // Stagger the attack delay so fangs snap shut in a wave
            fang.setAttackDelay((i - 1) * 2);
        }

        // Final burst at the end of the fang line
        Location endPoint = base.clone().add(direction.clone().multiply((numFangs + 0.5) * spacing));
        world.spawnParticle(Particle.EXPLOSION_EMITTER, endPoint, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.DRAGON_BREATH, endPoint, 35, 0.7, 0.7, 0.7, 0.06);
        world.spawnParticle(Particle.WITCH, endPoint, 50, 0.8, 0.8, 0.8, 0.02);
    }
}
