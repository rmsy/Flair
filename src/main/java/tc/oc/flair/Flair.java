package tc.oc.flair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import com.sk89q.bukkit.util.BukkitCommandsManager;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.*;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Flair extends JavaPlugin {

    private static final @Nonnull FlairManager flairManager = new FlairManager(null);
    private static @Nullable Flair plugin;
    private @Nonnull CommandsManager commands;
    private @Nonnull CommandsManagerRegistration commandsRegistration;

    public static FlairManager getManager() {
        return flairManager;
    }
    
    @Override
    public void onDisable() {
        this.commandsRegistration.unregisterCommands();
        this.commandsRegistration = null;
        this.commands = null;
        Flair.plugin = null;
    }

    @Override
    public void onEnable() {
        Flair.plugin = this;

        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.reloadConfig();
        
        FlairManager.parseConfiguration(this.getConfig());

        this.commands = new BukkitCommandsManager();
        this.commandsRegistration = new CommandsManagerRegistration(this, this.commands);
        this.commandsRegistration.register(Flair.class);
    }

    
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        try {
            this.commands.execute(command.getName(), args, sender, sender);
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Usage: " + e.getUsage());
        } catch (WrappedCommandException e) {
            sender.sendMessage(ChatColor.RED + "An unknown error has occurred. Please notify an administrator.");
            e.printStackTrace();
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }

        return true;
    }

    @Command(
            aliases = {"flair"},
            desc = "Reloads flairs.",
            max = 0
    )
    @CommandPermissions({"flair.reload"})
    @Console
    public static void onFlairCommand(@Nonnull final CommandContext arguments, @Nonnull final CommandSender sender) {
        Flair.plugin.reloadConfig();
        FlairManager.parseConfiguration(Flair.plugin.getConfig());
        Preconditions.checkNotNull(sender, "Sender").sendMessage(ChatColor.GREEN + "Config reloaded.");
    }

}
