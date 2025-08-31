package me.nd.leilao.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import static me.nd.leilao.cmd.Leilao.deliverPendingItems;

public class PlayerJoin implements Listener {
	
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        deliverPendingItems(player);
    }

}
