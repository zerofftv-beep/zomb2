package com.dayzombies.tasks;

import com.dayzombies.DayZZombies;
import com.dayzombies.config.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ZombieSpawnerTask extends BukkitRunnable {

    private final DayZZombies plugin;
    private final Map<UUID, Long> lastSpawnTime = new HashMap<>();
    private final Random random = new Random();

    public ZombieSpawnerTask(DayZZombies plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            WorldConfig worldConfig = plugin.getWorldConfig(world);

            if (!worldConfig.isEnabled() || !worldConfig.isCustomSpawningEnabled()) {
                continue;
            }

            long lastTime = lastSpawnTime.getOrDefault(player.getUniqueId(), 0L);
            long cooldownMs = worldConfig.getSpawnCooldown() * 1000L;

            if (now - lastTime < cooldownMs) {
                continue;
            }

            int maxRadius = worldConfig.getMaxSpawnRadius();
            int minRadius = worldConfig.getMinSpawnRadius();
            int maxZombies = worldConfig.getMaxZombiesNearPlayer();

            // Подсчет текущего количества зомби вокруг игрока
            int currentZombies = 0;
            for (Entity entity : player.getNearbyEntities(maxRadius, maxRadius, maxRadius)) {
                if (entity.getType() == EntityType.ZOMBIE) {
                    currentZombies++;
                }
            }

            if (currentZombies >= maxZombies) {
                continue;
            }

            // Спавним от 1 до 3 зомби за раз, чтобы пополнить популяцию
            int toSpawn = Math.min(random.nextInt(3) + 1, maxZombies - currentZombies);

            boolean spawnedAny = false;
            for (int i = 0; i < toSpawn; i++) {
                Location spawnLoc = findSpawnLocation(player, world, minRadius, maxRadius, worldConfig.isSpawnInSunlight());
                if (spawnLoc != null) {
                    world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
                    spawnedAny = true;
                }
            }

            if (spawnedAny) {
                lastSpawnTime.put(player.getUniqueId(), now);
            }
        }
    }

    private Location findSpawnLocation(Player player, World world, int minRadius, int maxRadius, boolean spawnInSunlight) {
        Location pLoc = player.getLocation();
        
        // Делаем до 5 попыток найти безопасное место для спавна
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            int radiusDelta = Math.max(1, maxRadius - minRadius);
            double r = minRadius + random.nextInt(radiusDelta);

            int x = pLoc.getBlockX() + (int) (r * Math.cos(angle));
            int z = pLoc.getBlockZ() + (int) (r * Math.sin(angle));

            int highestY = world.getHighestBlockYAt(x, z);
            int y = highestY + 1;

            // Если игрок находится глубоко под землей (в пещере/бункере), пробуем спавнить на уровне игрока
            if (pLoc.getY() < highestY - 15) {
                y = pLoc.getBlockY() + (random.nextInt(5) - 2);
            }

            Location loc = new Location(world, x + 0.5, y, z + 0.5);

            // Проверка условий освещения (если спавн на солнце запрещен)
            if (!spawnInSunlight) {
                Block block = loc.getBlock();
                boolean isDay = world.getTime() < 12300 || world.getTime() > 23850;
                if (isDay && block.getLightFromSky() > 8) {
                    continue; // Слишком светло от солнца, ищем другое место
                }
            }

            // Проверка безопасности блока (не в блоке, не в лаве/воде)
            Block foot = loc.getBlock();
            Block head = loc.clone().add(0, 1, 0).getBlock();
            Block below = loc.clone().add(0, -1, 0).getBlock();

            if (isPassable(foot) && isPassable(head) && isSolid(below)) {
                return loc;
            }
        }

        return null;
    }

    private boolean isPassable(Block block) {
        Material mat = block.getType();
        return !mat.isSolid() && mat != Material.LAVA && mat != Material.WATER;
    }

    private boolean isSolid(Block block) {
        Material mat = block.getType();
        return mat.isSolid() && mat != Material.LAVA && mat != Material.BARRIER && mat != Material.BEDROCK;
    }
}
