package me.nd.leilao.listener;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.leilao.Main;
import me.nd.leilao.dados.Auction;
import static me.nd.leilao.cmd.Leilao.auctions;
import static me.nd.leilao.cmd.Leilao.pendingBids;
import static me.nd.leilao.cmd.Leilao.playerPages;
import static me.nd.leilao.cmd.Leilao.ITEMS_PER_PAGE;
import static me.nd.leilao.cmd.Leilao.pendingItems;
import static me.nd.leilao.cmd.Leilao.AUCTION_SLOTS;
import static me.nd.leilao.cmd.Leilao.openAuctionMenu;

public class Inventory implements Listener {
	
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().startsWith("Leilão - Página ")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0) return; // Ignore clicks outside the inventory

        int page = playerPages.getOrDefault(player.getName(), 0);

        // Navegação
        if (slot == 18 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            openAuctionMenu(player, page - 1);
            return;
        }

        if (slot == 26 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            openAuctionMenu(player, page + 1);
            return;
        }

        // Ignora clique no item de informação ou fora dos slots de leilão
        if (slot == 4 || !isAuctionSlot(slot)) return;

        // Calcula o índice do leilão com base no slot
        int slotIndex = getSlotIndex(slot);
        if (slotIndex == -1) return;
        int index = page * ITEMS_PER_PAGE + slotIndex;
        if (index >= auctions.size()) return;

        Auction auction = auctions.get(index);
        if (auction.seller.equals(player.getName())) {
            player.sendMessage("§cVocê não pode dar lance no seu próprio leilão!");
            return;
        }

        pendingBids.put(player.getName(), index);
        player.sendMessage("§eDigite o valor que deseja dar de lance.");
        player.sendMessage("§eValor atual: §f" + auction.currentBid);
        player.sendMessage("");
        player.sendMessage("§cAo dar o lance o valor é debitado da sua conta!");
        player.sendMessage("§cCaso não ganhe o leilão o dinheiro perdido retorna!");
        player.closeInventory();

        // Timeout para remover lance pendente após 10 segundos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingBids.remove(player.getName()) != null) {
                    player.sendMessage("§cTempo esgotado para dar lance!");
                }
            }
        }.runTaskLater(Main.get(), 200L); // 10 segundos (20 ticks/segundo * 10)
    }
    
    // Verifica se o slot é um dos slots de leilão
    private boolean isAuctionSlot(int slot) {
        for (int auctionSlot : AUCTION_SLOTS) {
            if (slot == auctionSlot) return true;
        }
        return false;
    }

    // Obtém o índice do slot na lista AUCTION_SLOTS
    private int getSlotIndex(int slot) {
        for (int i = 0; i < AUCTION_SLOTS.length; i++) {
            if (AUCTION_SLOTS[i] == slot) return i;
        }
        return -1;
    }
    
    public static void addPendingItem(String playerName, ItemStack item, String message) {
        List<ItemStack> items = pendingItems.computeIfAbsent(playerName, k -> new ArrayList<>());
        items.add(item.clone());
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.sendMessage(message);
            player.sendMessage("§eO item será entregue quando você tiver espaço no inventário!");
        }
    }

}
