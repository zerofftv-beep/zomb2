package com.dayzombies.listeners;

import com.dayzombies.DayZZombies;
import com.dayzombies.config.WorldConfig;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class ZombieSpawnListener implements Listener {

    private final DayZZombies plugin;
    private final Random random = new Random();

    public ZombieSpawnListener(DayZZombies plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        WorldConfig worldConfig = plugin.getWorldConfig(event.getEntity().getWorld());
        if (!worldConfig.isEnabled()) {
            return;
        }

        // Отключение остальных мобов кроме зомби
        if (worldConfig.isDisableOtherMobs()) {
            if (event.getEntity() instanceof Mob && event.getEntityType() != EntityType.ZOMBIE) {
                CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
                // Отменяем естественные спавны других мобов
                if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                    reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN ||
                    reason == CreatureSpawnEvent.SpawnReason.SPAWNER ||
                    reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS ||
                    reason == CreatureSpawnEvent.SpawnReason.PATROL ||
                    reason == CreatureSpawnEvent.SpawnReason.RAID ||
                    reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION ||
                    reason == CreatureSpawnEvent.SpawnReason.NETHER_PORTAL) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Настройка параметров спавнящегося зомби
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();

            // Установка урона
            AttributeInstance damageAttr = zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(worldConfig.getDamage());
            }

            // Установка базового атрибута брони
            AttributeInstance armorAttr = zombie.getAttribute(Attribute.GENERIC_ARMOR);
            if (armorAttr != null) {
                armorAttr.setBaseValue(worldConfig.getArmorAttribute());
            }

            // Настройка экипировки (брони)
            EntityEquipment equipment = zombie.getEquipment();
            if (equipment != null) {
                if (random.nextDouble() < worldConfig.getArmorEquipChance()) {
                    equipRandomArmor(equipment);
                } else {
                    equipment.clear();
                }
            }
        }
    }

    // Обработка горения от солнца (Уровень 1: Событие возгорания)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onZombieCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            WorldConfig worldConfig = plugin.getWorldConfig(zombie.getWorld());
            if (!worldConfig.isEnabled()) {
                return;
            }

            // Если горение от солнца отключено
            if (!worldConfig.isSunBurn()) {
                // Если причина горения не другой моб и не блок (лава/огонь), значит это солнце
                if (!(event instanceof EntityCombustByEntityEvent) && !(event instanceof EntityCombustByBlockEvent)) {
                    event.setCancelled(true);
                    zombie.setFireTicks(0); // Принудительно тушим для Arclight/Forge
                }
            }
        }
    }

    // Обработка горения от солнца (Уровень 2: Надежная защита от урона огнем в Arclight/Forge)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onZombieDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            EntityDamageEvent.DamageCause cause = event.getCause();

            // Если зомби получает урон от огня или горения
            if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.BURN) {
                WorldConfig worldConfig = plugin.getWorldConfig(zombie.getWorld());
                if (!worldConfig.isEnabled() || worldConfig.isSunBurn()) {
                    return;
                }

                // Проверяем, стоит ли зомби физически в огне, костре или лаве
                Material currentBlock = zombie.getLocation().getBlock().getType();
                Material blockBelow = zombie.getLocation().clone().add(0, -1, 0).getBlock().getType();

                boolean inFireOrLava = isFireOrLava(currentBlock) || isFireOrLava(blockBelow);

                // Если зомби НЕ находится в костре/огне/лаве, значит он горит от солнца
                if (!inFireOrLava) {
                    event.setCancelled(true); // Отменяем урон
                    zombie.setFireTicks(0);   // Мгновенно сбрасываем таймер горения
                }
            }
        }
    }

    private boolean isFireOrLava(Material mat) {
        return mat == Material.FIRE || mat == Material.SOUL_FIRE || 
               mat == Material.LAVA || mat == Material.CAMPFIRE || 
               mat == Material.SOUL_CAMPFIRE;
    }

    private void equipRandomArmor(EntityEquipment equipment) {
        int tier = random.nextInt(4);
        switch (tier) {
            case 0: // Кожаная
                equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                equipment.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.LEATHER_BOOTS));
                break;
            case 1: // Кольчужная
                equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                equipment.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                break;
            case 2: // Золотая
                equipment.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                equipment.setChestplate(new ItemStack(Material.GOLDEN_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.GOLDEN_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.GOLDEN_BOOTS));
                break;
            case 3: // Железная
                equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
                equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                equipment.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                equipment.setBoots(new ItemStack(Material.IRON_BOOTS));
                break;
        }
    }
}
