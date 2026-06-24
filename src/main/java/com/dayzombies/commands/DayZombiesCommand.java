package com.dayzombies.commands;

import com.dayzombies.DayZZombies;
import com.dayzombies.config.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DayZombiesCommand implements CommandExecutor, TabCompleter {

    private final DayZZombies plugin;
    private final List<String> noiseTypes = Arrays.asList(
            "sprint", "jump", "eat", "tacz_shoot", "break_block", 
            "place_block", "open_chest_door", "item_drop", "take_damage"
    );

    public DayZombiesCommand(DayZZombies plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        // В Arclight/Forge проверка isOp() работает надежнее, чем проверка разрешений Bukkit
        return sender.isOp() || sender.hasPermission("dayzzombies.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав (OP или dayzzombies.admin) для использования этой команды.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "[DayZZombies] Конфигурация успешно перезагружена!");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            String worldName = (args.length > 1) ? args[1] : getSenderWorldName(sender);
            sendWorldInfo(sender, worldName);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Недостаточно аргументов! Использование: /" + label + " set <мир> <параметр> <значение>");
                return true;
            }

            String worldName = args[1];
            String param = args[2].toLowerCase();
            WorldConfig worldConfig = plugin.getWorldConfig(worldName);

            try {
                switch (param) {
                    case "sunburn":
                        boolean sunBurn = parseBoolean(args[3]);
                        worldConfig.setSunBurn(sunBurn);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " горение от солнца установлено на: " + sunBurn);
                        break;
                    case "damage":
                        double damage = Double.parseDouble(args[3]);
                        worldConfig.setDamage(damage);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " урон зомби установлен на: " + damage);
                        break;
                    case "armor":
                        double armor = Double.parseDouble(args[3]);
                        worldConfig.setArmorAttribute(armor);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " показатель брони зомби установлен на: " + armor);
                        break;
                    case "armorchance":
                        double armorChance = Double.parseDouble(args[3]);
                        worldConfig.setArmorEquipChance(armorChance);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " шанс наличия экипировки брони установлен на: " + armorChance);
                        break;
                    case "spawninsun":
                        boolean spawnInSun = parseBoolean(args[3]);
                        worldConfig.setSpawnInSunlight(spawnInSun);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " спавн на солнце установлен на: " + spawnInSun);
                        break;
                    case "spawnradius":
                        if (args.length < 5) {
                            sender.sendMessage(ChatColor.RED + "Укажите минимальный и максимальный радиус: /" + label + " set " + worldName + " spawnradius <min> <max>");
                            return true;
                        }
                        int minRad = Integer.parseInt(args[3]);
                        int maxRad = Integer.parseInt(args[4]);
                        worldConfig.setMinSpawnRadius(minRad);
                        worldConfig.setMaxSpawnRadius(maxRad);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " радиус спавна установлен от " + minRad + " до " + maxRad);
                        break;
                    case "spawncooldown":
                        int cd = Integer.parseInt(args[3]);
                        worldConfig.setSpawnCooldown(cd);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " кулдаун спавна установлен на: " + cd + " сек.");
                        break;
                    case "maxzombies":
                        int maxZ = Integer.parseInt(args[3]);
                        worldConfig.setMaxZombiesNearPlayer(maxZ);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " макс. зомби возле игрока установлено на: " + maxZ);
                        break;
                    case "disableothermobs":
                        boolean disable = parseBoolean(args[3]);
                        worldConfig.setDisableOtherMobs(disable);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " отключение остальных мобов установлено на: " + disable);
                        break;
                    case "hearing":
                        if (args.length < 5) {
                            sender.sendMessage(ChatColor.RED + "Укажите тип шума и радиус: /" + label + " set " + worldName + " hearing <тип_шума> <радиус>");
                            return true;
                        }
                        String noiseType = args[3].toLowerCase();
                        if (!noiseTypes.contains(noiseType)) {
                            sender.sendMessage(ChatColor.RED + "Неизвестный тип шума! Доступные: " + String.join(", ", noiseTypes));
                            return true;
                        }
                        double hearingRad = Double.parseDouble(args[4]);
                        worldConfig.setHearingRadius(noiseType, hearingRad);
                        sender.sendMessage(ChatColor.GREEN + "[DayZZombies] В мире " + worldName + " радиус слышимости для '" + noiseType + "' установлен на: " + hearingRad);
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Неизвестный параметр! Наберите /" + label + " help для справки.");
                        break;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Неверный формат числа!");
            }
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private boolean parseBoolean(String s) {
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("on");
    }

    private String getSenderWorldName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getWorld().getName();
        }
        return Bukkit.getWorlds().get(0).getName();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "=== Управление DayZZombies (Arclight) ===");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " help " + ChatColor.WHITE + "- Показать эту справку");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload " + ChatColor.WHITE + "- Перезагрузить конфиг");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info [мир] " + ChatColor.WHITE + "- Показать настройки мира");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> sunburn <true/false> " + ChatColor.WHITE + "- Горение от солнца");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> damage <число> " + ChatColor.WHITE + "- Урон зомби");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> armor <число> " + ChatColor.WHITE + "- Атрибут брони зомби");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> armorchance <0.0-1.0> " + ChatColor.WHITE + "- Шанс экипировки брони");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> spawninsun <true/false> " + ChatColor.WHITE + "- Спавн на солнце");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> spawnradius <min> <max> " + ChatColor.WHITE + "- Радиус спавна от игрока");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> spawncooldown <секунды> " + ChatColor.WHITE + "- Кулдаун спавна");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> maxzombies <число> " + ChatColor.WHITE + "- Макс. зомби возле игрока");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> disableothermobs <true/false> " + ChatColor.WHITE + "- Отключение остальных мобов");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <мир> hearing <тип_шума> <радиус> " + ChatColor.WHITE + "- Радиус слуха зомби");
    }

    private void sendWorldInfo(CommandSender sender, String worldName) {
        WorldConfig wc = plugin.getWorldConfig(worldName);
        sender.sendMessage(ChatColor.GOLD + "=== Настройки DayZZombies для мира: " + ChatColor.AQUA + worldName + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Включен: " + ChatColor.WHITE + wc.isEnabled());
        sender.sendMessage(ChatColor.YELLOW + "Горение от солнца (sunburn): " + ChatColor.WHITE + wc.isSunBurn());
        sender.sendMessage(ChatColor.YELLOW + "Урон зомби (damage): " + ChatColor.WHITE + wc.getDamage());
        sender.sendMessage(ChatColor.YELLOW + "Атрибут брони (armor): " + ChatColor.WHITE + wc.getArmorAttribute());
        sender.sendMessage(ChatColor.YELLOW + "Шанс брони (armorchance): " + ChatColor.WHITE + wc.getArmorEquipChance());
        sender.sendMessage(ChatColor.YELLOW + "Спавн на солнце (spawninsun): " + ChatColor.WHITE + wc.isSpawnInSunlight());
        sender.sendMessage(ChatColor.YELLOW + "Радиус спавна (spawnradius): " + ChatColor.WHITE + wc.getMinSpawnRadius() + " - " + wc.getMaxSpawnRadius());
        sender.sendMessage(ChatColor.YELLOW + "Кулдаун спавна (spawncooldown): " + ChatColor.WHITE + wc.getSpawnCooldown() + " сек.");
        sender.sendMessage(ChatColor.YELLOW + "Макс. зомби у игрока (maxzombies): " + ChatColor.WHITE + wc.getMaxZombiesNearPlayer());
        sender.sendMessage(ChatColor.YELLOW + "Отключение других мобов (disableothermobs): " + ChatColor.WHITE + wc.isDisableOtherMobs());
        sender.sendMessage(ChatColor.YELLOW + "--- Радиусы слуха зомби ---");
        for (String noise : noiseTypes) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + noise + ": " + ChatColor.WHITE + wc.getHearingRadius(noise));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!hasAdminPermission(sender)) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload", "info", "set");
            return filterPrefix(subCommands, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("set"))) {
            List<String> worldNames = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
            worldNames.add("default");
            return filterPrefix(worldNames, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> params = Arrays.asList(
                    "sunburn", "damage", "armor", "armorchance", "spawninsun", 
                    "spawnradius", "spawncooldown", "maxzombies", "disableothermobs", "hearing"
            );
            return filterPrefix(params, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            String param = args[2].toLowerCase();
            if (param.equals("sunburn") || param.equals("spawninsun") || param.equals("disableothermobs")) {
                return filterPrefix(Arrays.asList("true", "false"), args[3]);
            } else if (param.equals("hearing")) {
                return filterPrefix(noiseTypes, args[3]);
            } else if (param.equals("damage") || param.equals("armor")) {
                return Arrays.asList("2.0", "4.0", "6.0", "8.0", "10.0");
            } else if (param.equals("armorchance")) {
                return Arrays.asList("0.0", "0.25", "0.5", "0.75", "1.0");
            }
        }

        return completions;
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
