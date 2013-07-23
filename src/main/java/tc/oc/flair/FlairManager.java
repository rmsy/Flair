package tc.oc.flair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Utility methods for managing a player's flair.
 *
 * Priority specifies what order the flair colors should be resolved in. By
 * default this is the natural ordering of {@link ChatColor}, but users may
 * provide their own list specifying the order.
 */
public class FlairManager {
    private static @Nonnull HashMap<String, String> flairs = new HashMap<String, String>();
    private static @Nonnull HashMap<String, List<ChatColor>> flairColorPrioritys = new HashMap<String, List<ChatColor>>();
    private static @Nonnull List<String> flairOrder = new ArrayList<String>();

    /**
     * Creates an instance with natural priority.
     */
    public FlairManager(String flair) {
        if(flair != null) {
            FlairManager.flairColorPrioritys.put(flair, ImmutableList.copyOf(getFullColorSet()));
        }
    }

    /**
     * Creates an instance with custom priority where omitted colors come after
     * the ones specified.
     *
     * @param priority Specified color priority list
     *
     * @throws NullPointerException if priority is null
     *
     * @see #setPriority(List, DefaultPriority)
     */
    public FlairManager(@Nonnull String flair, @Nonnull List<ChatColor> priority) {
        this(flair, priority, DefaultPriority.AFTER);
    }

    /**
     * Creates an instance with custom priority and specified default priority.
     *
     * @param priority Specified color priority list
     * @param defaultPriority Specified default priority
     *
     * @throws NullPointerException if priority or defaultPriority are null
     *
     * @see #setPriority(List, DefaultPriority)
     */
    public FlairManager(@Nonnull String flair, @Nonnull List<ChatColor> priority, @Nonnull DefaultPriority defaultPriority) {
        this.setPriority(flair, priority, defaultPriority);
    }

    public static void parseConfiguration(@Nonnull Configuration config) {
        Preconditions.checkNotNull(config, "configuration");
        ConfigurationSection flairsConfig = (ConfigurationSection) config.get("flairs");

        for(String flair : flairsConfig.getKeys(false)) {
            FlairManager flairManager = new FlairManager(flair);
            ConfigurationSection flairConfig = (ConfigurationSection) flairsConfig.get(flair);

            FlairManager.flairs.put(flair, flairConfig.getString("flair", "*"));
            
            DefaultPriority defaultPriority = flairConfig.getString("default", "after").equalsIgnoreCase("before") ? DefaultPriority.BEFORE : DefaultPriority.AFTER;

            List<String> rawPriority = flairConfig.getStringList("priority");
            List<ChatColor> priority = Lists.newArrayListWithCapacity(rawPriority.size());
            for(String str : rawPriority) {
                ChatColor c = parseChatColor(str);
                if(c != null) {
                    priority.add(c);
                } else {
                    Bukkit.getLogger().warning("Failed to parse '" + str + "' color priority for flair " + flairConfig.getName());
                }
            }

            flairManager.setPriority(flair, priority, defaultPriority);
        }

        FlairManager.flairOrder = config.getStringList("order");
    }

    /**
     * Gets a string containing the player's given flair as determined by
     * its permissions.
     *
     * The string will be formed by looping through the available colors in the
     * order specified by {@link #getPriority} then concatenating them to the
     * result followed by the flair.
     *
     * @param player Permissible player to get flair for
     * @return String containing different colored flairs (never null)
     *
     * @throws NullPointerException if player is null
     */
    public @Nonnull String getFlairs(@Nonnull Permissible player) {
        Preconditions.checkNotNull(player, "player");

        StringBuilder nameFlair = new StringBuilder();
        for(String flair : flairOrder) {
            for(ChatColor c : FlairManager.flairColorPrioritys.get(flair)) {
                if(c.isColor() && this.hasPermission(player, flair, c)) {
                    nameFlair.append(c).append(FlairManager.flairs.get(flair));
                }
            }
        }


        return nameFlair.toString();
    }

    /**
     * Checks to see whether the given permissible player has permission for
     * the given flair color.
     *
     * @param player Permissible player to check permission for
     * @param color Flair color to check
     * @return true if the player has permission, false otherwise
     *
     * @throws NullPointerException if player or color is null
     * @throws IllegalArgumentException if color is not a color
     */
    public boolean hasPermission(@Nonnull Permissible player, @Nonnull String flair, @Nonnull ChatColor color) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(flair, "flair");
        Preconditions.checkNotNull(color, "color");
        Preconditions.checkArgument(color.isColor(), "color must be a color");

        return player.hasPermission(this.getFlairPermissionNode(flair, color));
    }

    /**
     * Gets the permission node for the given flair color.
     *
     * @param color Color to get permission node for
     * @return {@link Permission} instance that represents the flair color
     *
     * @throws NullPointerException if color is null
     * @throws
     */
    public @Nonnull Permission getFlairPermissionNode(@Nonnull String flair, @Nonnull ChatColor color) {
        Preconditions.checkNotNull(flair, "flair");
        Preconditions.checkNotNull(color, "color");
        Preconditions.checkArgument(color.isColor(), "color must be a color");

        return new Permission("flair." + flair + "." + color.name().toLowerCase(), PermissionDefault.FALSE);
    }

    /**
     * Gets the priority that this manager uses to resolve colors.
     *
     * @return Full list of all colors excluding formats that describe the
     *         current priority
     */
    public @Nonnull List<ChatColor> getPriority(@Nonnull String flair) {
        Preconditions.checkNotNull(flair, "flair");
        
        return FlairManager.flairColorPrioritys.get(flair);
    }

    /**
     * Sets the priority used to resolve flair colors with any omitted
     * colors coming after the given ones.
     *
     * @param priority Specified color priority list
     * @return Old color priority list (never null)
     *
     * @throws NullPointerException if priority is null
     */
    public @Nonnull List<ChatColor> setPriority(@Nonnull String flair, @Nonnull List<ChatColor> priority) {
        return this.setPriority(flair, priority, DefaultPriority.AFTER);
    }

    /**
     * Sets the priority used to resolve flair colors with any omitted
     * colors coming either before or after the given ones in their natural
     * order as specified by the default priority
     *
     * Implementation note: Formatting chat colors will automatically be
     * removed from the set priority. Only the first instance of each color
     * specified in the priority list will be respected.
     *
     * @param priority Specified color priority list
     * @param defaultPriority Default priority used for omitted colors
     * @return Old color priority list (never null)
     *
     * @throws NullPointerException if priority or defaultPriority are null
     */
    public @Nonnull List<ChatColor> setPriority(@Nonnull String flair, @Nonnull List<ChatColor> priority, @Nonnull DefaultPriority defaultPriority) {
        Preconditions.checkNotNull(priority, "priority");

        Set<ChatColor> remaining = getFullColorSet();
        List<ChatColor> newPriority = Lists.newArrayListWithCapacity(priority.size());
        for(ChatColor c : priority) {
            if(c.isColor() && remaining.contains(c)) {
                newPriority.add(c);
                remaining.remove(c);
            }
        }

        switch(defaultPriority) {
        case BEFORE:
            newPriority.addAll(0, remaining); // add to front
            break;
        case AFTER:
            newPriority.addAll(remaining); // add to back
            break;
        }

        List<ChatColor> old = FlairManager.flairColorPrioritys.get(flair);
        FlairManager.flairColorPrioritys.put(flair, ImmutableList.copyOf(newPriority));
        return old;
    }

    /** Gets the full set of chat colors excluding formatting codes */
    private static @Nonnull Set<ChatColor> getFullColorSet() {
        Set<ChatColor> colors = EnumSet.allOf(ChatColor.class);
        for(Iterator<ChatColor> it = colors.iterator(); it.hasNext(); ) {
            ChatColor c = it.next();
            if(!c.isColor()) {
                it.remove();
            }
        }
        return colors;
    }
    
    public static ChatColor parseChatColor(String text) {
        if(text == null) {
            return null;
        }
        for(ChatColor color : ChatColor.values()) {
            if(text.equalsIgnoreCase(color.name().replace("_", " "))) {
                return color;
            }
        }
        return null;
    }

    /**
     * Specifies where omitted chat colors will come in color pririty lists.
     */
    public static enum DefaultPriority {
        BEFORE,
        AFTER
    }
}
