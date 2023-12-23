package net.chinesecommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class ChineseCommand extends JavaPlugin implements Listener {

    private List<String> commandList = new ArrayList<>();
    private List<String> chatList = new ArrayList<>();
    private Map<String, String> commandMap = new HashMap<>();
    private Map<String, String> shortcutCommandMap = new HashMap<>();

    private File file = null;
    private FileConfiguration config = null;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        loadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cncmd")) {
            if (!sender.isOp()) {
                sender.sendMessage(colorCode("&c你没有权限这样做!"));
                return true;
            }

            if (args.length == 1) {
                loadConfig();
                sender.sendMessage(colorCode("&a配置已重载!"));
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.isCancelled() && !commandMap.isEmpty()) {
            String chat = ChatColor.stripColor(event.getMessage().trim());
            String[] chatParts = chat.split(" ", 2);
            String command = chatParts[0];

            if (commandMap.containsKey(command)) {
                handleCommand(event.getPlayer(), command, chatParts.length > 1 ? chatParts[1] : "");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUseCommand(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled() && !shortcutCommandMap.isEmpty()) {
            String command = event.getMessage().split(" ", 2)[0];

            if (shortcutCommandMap.containsKey(command)) {
                handleCommand(event.getPlayer(), command, event.getMessage().replaceFirst(command, "").trim());
                event.setCancelled(true);
            }
        }
    }

    private void handleCommand(org.bukkit.entity.Player player, String command, String args) {
        String fullCommand = commandMap.get(command).toLowerCase().replace("%player%", player.getName());

        getLogger().info("Full Command: " + fullCommand + args);

        if (fullCommand.startsWith("op-")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fullCommand.replace("op-", "") + args);
            getLogger().info(player.getName() + " issued console command: " + fullCommand.replace("op-", "") + args);
        } else {
            player.performCommand(fullCommand + args);
            getLogger().info(player.getName() + " issued chinese command: /" + fullCommand + args);
        }
    }

    private void loadConfig() {
        getDataFolder().mkdir();
        file = new File(getDataFolder(), "setting.yml");
        config = load(file);

        commandList = config.getStringList("CommandList");
        chatList = config.getStringList("ChatList");

        commandMap.clear();
        shortcutCommandMap.clear();

        for (String entry : commandList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String command = parts[0].replaceAll("^/+", "").replaceAll("/+$", "");
                shortcutCommandMap.put(command, parts[1].trim());

                getLogger().info("Shortcut Command: " + command + " - " + parts[1].trim());
            }
        }

        for (String entry : chatList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String command = parts[0].replaceAll("^/+", "").replaceAll("/+$", "");
                commandMap.put(command, parts[1].trim());

                getLogger().info("Chat Command: " + command + " - " + parts[1].trim());
            }
        }
    }

    private FileConfiguration load(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileConfiguration config = load(file);
                config.set("ChatList", Arrays.asList("例子:/spawn", "/例子2:/spawn", "/例子3:OP-tell %player%"));
                config.set("CommandList", Arrays.asList("/例子4:/spawn", "/告诉:/tell"));
                config.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private String colorCode(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
