package com.dayzombies;

import com.dayzombies.commands.DayZombiesCommand;
import com.dayzombies.config.WorldConfig;
import com.dayzombies.listeners.PlayerNoiseListener;
import com.dayzombies.listeners.ZombieSpawnListener;
import com.dayzombies.tasks.ZombieSpawnerTask;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayZZombies extends JavaPlugin {

    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();
    private ZombieSpawnerTask spawnerTask;

    @Override
    public void onEnable() {
        // Сохраняем конфиг по умолчанию, если он не существует
        saveDefaultConfig();

        // Регистрация команд
        DayZombiesCommand commandExecutor = new DayZombiesCommand(this);
        if (getCommand("dayzombies") != null) {
            getCommand("dayzombies").setExecutor(commandExecutor);
            getCommand("dayzombies").setTabCompleter(commandExecutor);
        }

        // Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(new ZombieSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerNoiseListener(this), this);

        // Запуск задачи спавнера (проверяет каждую секунду / 20 тиков)
        spawnerTask = new ZombieSpawnerTask(this);
        spawnerTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("DayZZombies успешно включен! (Core: Arclight 1.20.1)");
    }

    @Override
    public void onDisable() {
        if (spawnerTask != null) {
            spawnerTask.cancel();
        }
        worldConfigs.clear();
        getLogger().info("DayZZombies выключен.");
    }

    public void reloadPlugin() {
        reloadConfig();
        worldConfigs.clear();
    }

    public WorldConfig getWorldConfig(World world) {
        return getWorldConfig(world.getName());
    }

    public WorldConfig getWorldConfig(String worldName) {
        return worldConfigs.computeIfAbsent(worldName, name -> new WorldConfig(this, name));
    }

    public List<String> getTaczWeaponKeywords() {
        return getConfig().getStringList("tacz_settings.weapon_keywords");
    }

    public long getTaczShootCooldownMs() {
        return getConfig().getLong("tacz_settings.shoot_sound_cooldown_ms", 500L);
    }
}
