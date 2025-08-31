package me.nd.leilao.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.leilao.Main;
import static me.nd.leilao.cmd.Leilao.auctions;
import static me.nd.leilao.cmd.Leilao.pendingBids;
import static me.nd.leilao.Main.economy;

import me.nd.leilao.dados.Auction;
import static me.nd.leilao.dados.Auction.saveAuctions;;

public class ChatListener implements Listener {
	
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer index = pendingBids.remove(player.getName());
        if (index == null) return;

        event.setCancelled(true); // Impede que a mensagem apareça no chat público
        String message = event.getMessage().trim();

        try {
            double bid = Double.parseDouble(message);
            if (index < 0 || index >= auctions.size()) {
                player.sendMessage("§cLeilão inválido ou expirado!");
                return;
            }

            Auction auction = auctions.get(index);
            if (auction.seller.equals(player.getName())) {
                player.sendMessage("§cVocê não pode dar lance no seu próprio leilão!");
                return;
            }

            if (bid <= auction.currentBid) {
                player.sendMessage("§cO lance deve ser maior que o atual (§a$" + auction.currentBid + "§c)!");
                return;
            }

            if (!economy.has(player, bid)) {
                player.sendMessage("§cVocê não tem §a$" + bid + " §cpara dar este lance!");
                return;
            }

            // Processa o lance
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (auction.highestBidder != null) {
                        economy.depositPlayer(Bukkit.getOfflinePlayer(auction.highestBidder), auction.currentBid);
                    }

                    economy.withdrawPlayer(player, bid);
                    auction.bidderAmounts.put(player.getName(), bid); // Armazena o lance do jogador
                    auction.currentBid = bid;
                    auction.highestBidder = player.getName();
                    Bukkit.broadcastMessage("§eNovo lance de §f" + player.getName() + " §epor §a$" + bid + " §eno item §f" + auction.itemName);
                    saveAuctions();
                }
            }.runTask(Main.get()); // Executa transações na thread principal
        } catch (NumberFormatException e) {
            player.sendMessage("§cPor favor, insira um valor numérico válido!");
        }
    }

}
