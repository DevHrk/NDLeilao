package me.nd.leilao.listener;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import me.nd.leilao.Main;

public class Listeners {

	public static void setupListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		List<Class<? extends Listener>> listenerClasses = Arrays.asList(
				PlayerJoin.class,
				ChatListener.class,
				Inventory.class);
		
		listenerClasses.forEach(listenerClass -> {
			if (Listener.class.isAssignableFrom(listenerClass)) {
				try {
					Listener listenerInstance = listenerClass.getDeclaredConstructor().newInstance();
					pm.registerEvents(listenerInstance, Main.get());
				} catch (ReflectiveOperationException e) {
					Bukkit.getLogger().severe(
							"Failed to register listener: " + listenerClass.getSimpleName() + " - " + e.getMessage());
				}
			} else {
				Bukkit.getLogger().warning("Class " + listenerClass.getSimpleName() + " does not implement Listener!");
			}
		});

	}
}
