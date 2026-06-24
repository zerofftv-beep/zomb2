package com.dayzombies.listeners;

import com.dayzombies.DayZZombies;
import com.dayzombies.config.WorldConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNoiseListener implements Listener {

    private final DayZZombies plugin;
    private final Map<UUID, Long> lastMoveNoiseTime = new HashMap<>();
    private final Map<UUID, Long> lastShootNoiseTime = new HashMap<>();

    public PlayerNoiseListener(DayZZombies plugin) {
        this.plugin = plugin;
    }

    private void makeNoise(Player player, String noiseType) {
        WorldConfig worldConfig = plugin.getWorldConfig(player.getWorld());
        if (!worldConfig.isEnabled() || !worldConfig.isHearingEnabled()) {
            return;
        }

        double radius = worldConfig.getHearingRadius(noiseType);
        if (radius <= 0) {
            return;
        }

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Zombie) {
                Zombie zombie = (Zombie) entity;
                // Если у зомби еще нет цели или текущая цель дальше, переключаем на издавшего шум игрока
                if (zombie.getTarget() == null || zombie.getTarget().getLocation().distanceSquared(player.getLocation()) > player.getLocation().distanceSquared(zombie.getLocation())) {
                    zombie.setTarget(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || player.isFlying() || player.isSwimming()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        long now = System.currentTimeMillis();
        long lastTime = lastMoveNoiseTime.getOrDefault(player.getUniqueId(), 0L);

        // Троттлинг проверок движения (раз в 1 секунду), чтобы избежать лагов
        if (now - lastTime < 1000L) {
            return;
        }

        // Проверка прыжка
        if (to.getY() > from.getY() && (to.getY() - from.getY()) > 0.25) {
            lastMoveNoiseTime.put(player.getUniqueId(), now);
            makeNoise(player, "jump");
            return;
        }

        // Проверка быстрого бега (sprint)
        if (player.isSprinting() && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
            lastMoveNoiseTime.put(player.getUniqueId(), now);
            makeNoise(player, "sprint");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        makeNoise(event.getPlayer(), "eat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        // 1. Проверка взаимодействия с дверями, сундуками, люками (open_chest_door)
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                String matName = block.getType().name();
                if (matName.contains("CHEST") || matName.contains("DOOR") || matName.contains("GATE") || matName.contains("BARREL") || matName.contains("SHULKER_BOX")) {
                    makeNoise(player, "open_chest_door");
                }
            }
        }

        // 2. Проверка стрельбы из оружия TacZ (Timeless and Classics Guns)
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() != Material.AIR) {
                String itemName = item.getType().name().toUpperCase();
                // Также проверяем метаданные/lore/custom model data в случае кастомных названий в ядре Arclight
                boolean isGun = false;
                for (String keyword : plugin.getTaczWeaponKeywords()) {
                    if (itemName.contains(keyword.toUpperCase())) {
                        isGun = true;
                        break;
                    }
                }

                if (isGun) {
                    long now = System.currentTimeMillis();
                    long lastShoot = lastShootNoiseTime.getOrDefault(player.getUniqueId(), 0L);
                    if (now - lastShoot >= plugin.getTaczShootCooldownMs()) {
                        lastShootNoiseTime.put(player.getUniqueId(), now);
                        makeNoise(player, "tacz_shoot");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // На случай если Arclight транслирует выстрелы из модов или луков/арбалетов в это событие
            makeNoise(player, "tacz_shoot");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        makeNoise(event.getPlayer(), "break_block");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        makeNoise(event.getPlayer(), "place_block");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        makeNoise(event.getPlayer(), "item_drop");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            makeNoise(player, "take_damage");
        }
    }
}
