package me.nd.leilao;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import static me.nd.leilao.cmd.Commands.setupCommands;
import static me.nd.leilao.listener.Listeners.setupListeners;

public class Main extends JavaPlugin {
	
	public static Economy economy;
	
	@Override
	public void onEnable() {
		Bukkit.getServer().getConsoleSender().sendMessage("§a[NDLeilao] plugin iniciado");
		saveDefaultConfig();
		setupEconomy();
		setupCommands();
		setupListeners();
	}

	@Override
	public void onDisable() {
		Bukkit.getServer().getConsoleSender().sendMessage("§c[NDLeilao] plugin desligado");
	}
	
	public static Main get() {
        return (Main)JavaPlugin.getPlugin((Class)Main.class);
    }
	
    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return economy != null;
    }
	
}
