package eu.randomcrap.arrowmux;

import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ArrowMux extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final Server server = getServer();
        final PluginManager pm = server.getPluginManager();
        final Arrows arrows = new Arrows(this);
        pm.registerEvents(arrows, this);
        return;
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }
}
