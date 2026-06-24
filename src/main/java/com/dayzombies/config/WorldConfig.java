package com.dayzombies.config;

import com.dayzombies.DayZZombies;
import org.bukkit.configuration.ConfigurationSection;

public class WorldConfig {
    private final DayZZombies plugin;
    private final String worldName;

    public WorldConfig(DayZZombies plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    private String getPath(String subPath) {
        if (plugin.getConfig().contains("worlds." + worldName + "." + subPath)) {
            return "worlds." + worldName + "." + subPath;
        }
        return "worlds.default." + subPath;
    }

    private void setPath(String subPath, Object value) {
        String path = "worlds." + worldName + "." + subPath;
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }

    // --- General ---
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean(getPath("enabled"), true);
    }

    public void setEnabled(boolean enabled) {
        setPath("enabled", enabled);
    }

    // --- Zombie Settings ---
    public boolean isSunBurn() {
        return plugin.getConfig().getBoolean(getPath("zombie_settings.sun_burn"), false);
    }

    public void setSunBurn(boolean sunBurn) {
        setPath("zombie_settings.sun_burn", sunBurn);
    }

    public double getDamage() {
        return plugin.getConfig().getDouble(getPath("zombie_settings.damage"), 6.0);
    }

    public void setDamage(double damage) {
        setPath("zombie_settings.damage", damage);
    }

    public double getArmorAttribute() {
        return plugin.getConfig().getDouble(getPath("zombie_settings.armor_attribute"), 2.0);
    }

    public void setArmorAttribute(double armor) {
        setPath("zombie_settings.armor_attribute", armor);
    }

    public double getArmorEquipChance() {
        return plugin.getConfig().getDouble(getPath("zombie_settings.armor_equip_chance"), 0.25);
    }

    public void setArmorEquipChance(double chance) {
        setPath("zombie_settings.armor_equip_chance", chance);
    }

    // --- Spawner Settings ---
    public boolean isCustomSpawningEnabled() {
        return plugin.getConfig().getBoolean(getPath("spawner_settings.custom_spawning_enabled"), true);
    }

    public void setCustomSpawningEnabled(boolean enabled) {
        setPath("spawner_settings.custom_spawning_enabled", enabled);
    }

    public boolean isSpawnInSunlight() {
        return plugin.getConfig().getBoolean(getPath("spawner_settings.spawn_in_sunlight"), true);
    }

    public void setSpawnInSunlight(boolean spawnInSunlight) {
        setPath("spawner_settings.spawn_in_sunlight", spawnInSunlight);
    }

    public int getSpawnCooldown() {
        return plugin.getConfig().getInt(getPath("spawner_settings.spawn_cooldown"), 10);
    }

    public void setSpawnCooldown(int seconds) {
        setPath("spawner_settings.spawn_cooldown", seconds);
    }

    public int getMinSpawnRadius() {
        return plugin.getConfig().getInt(getPath("spawner_settings.min_spawn_radius"), 15);
    }

    public void setMinSpawnRadius(int radius) {
        setPath("spawner_settings.min_spawn_radius", radius);
    }

    public int getMaxSpawnRadius() {
        return plugin.getConfig().getInt(getPath("spawner_settings.max_spawn_radius"), 45);
    }

    public void setMaxSpawnRadius(int radius) {
        setPath("spawner_settings.max_spawn_radius", radius);
    }

    public int getMaxZombiesNearPlayer() {
        return plugin.getConfig().getInt(getPath("spawner_settings.max_zombies_near_player"), 15);
    }

    public void setMaxZombiesNearPlayer(int max) {
        setPath("spawner_settings.max_zombies_near_player", max);
    }

    public boolean isDisableOtherMobs() {
        return plugin.getConfig().getBoolean(getPath("spawner_settings.disable_other_mobs"), true);
    }

    public void setDisableOtherMobs(boolean disable) {
        setPath("spawner_settings.disable_other_mobs", disable);
    }

    // --- Hearing Settings ---
    public boolean isHearingEnabled() {
        return plugin.getConfig().getBoolean(getPath("hearing_settings.enabled"), true);
    }

    public void setHearingEnabled(boolean enabled) {
        setPath("hearing_settings.enabled", enabled);
    }

    public double getHearingRadius(String noiseType) {
        return plugin.getConfig().getDouble(getPath("hearing_settings.radius." + noiseType), 15.0);
    }

    public void setHearingRadius(String noiseType, double radius) {
        setPath("hearing_settings.radius." + noiseType, radius);
    }
}
