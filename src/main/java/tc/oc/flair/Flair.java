package tc.oc.flair;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Flair extends JavaPlugin {

    private static final @Nonnull FlairManager flairManager = new FlairManager(null);

    public static FlairManager getManager() {
        return flairManager;
    }
    
    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.reloadConfig();
        
        FlairManager.parseConfiguration(this.getConfig());
    }

    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!sender.hasPermission("flair.reload")) {
            sender.sendMessage(ChatColor.RED + "No permission");
            return true;
        }

        this.reloadConfig();
        FlairManager.parseConfiguration(this.getConfig());
        sender.sendMessage(ChatColor.GREEN + "Config reloaded");

        return true;
    }

}
