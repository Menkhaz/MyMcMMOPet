package io.github.takato65.mymcmmopet;

import de.Keyle.MyPet.MyPetApi;
import io.github.takato65.mymcmmopet.listeners.EntityListener;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;

public class MyMCMMOPet extends JavaPlugin {
    private EntityListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        double multiplier = getConfig().getDouble("xp-multiplier", 3.0);
        if (!Double.isFinite(multiplier) || multiplier < 0) {
            getLogger().severe("xp-multiplier must be finite and non-negative. Disabling the bridge.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!MyPetApi.isReady()) {
            getLogger().severe("MyPet 4's API is not ready. Disabling the bridge.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        listener = new EntityListener(this, new PetAttackResolver(MyPetApi.getPetManager()),
                new CombatExperienceService(multiplier, getConfig().getBoolean("allow-pvp-xp", false),
                        new HashSet<>(getConfig().getStringList("disabled-worlds"))));
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Taming XP bridge enabled for MyPet "
                + getServer().getPluginManager().getPlugin("MyPet").getDescription().getVersion()
                + " and mcMMO "
                + getServer().getPluginManager().getPlugin("mcMMO").getDescription().getVersion()
                + " (multiplier " + multiplier + ").");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.close();
        }
    }
}
