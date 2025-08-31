package me.nd.leilao.cmd;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.leilao.Main;
import me.nd.leilao.dados.Auction;
import static me.nd.leilao.dados.Auction.loadAuctions;
import static me.nd.leilao.dados.Auction.saveAuctions;
import static me.nd.leilao.listener.Inventory.addPendingItem;
import static me.nd.leilao.Main.economy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

public class Leilao extends Commands {
	public final static List<Auction> auctions = new ArrayList<>();
    public final static Map<String, Integer> playerPages = new HashMap<>();
    public final static Map<String, Integer> pendingBids = new HashMap<>(); // Jogador -> índice do leilão
    public final static Map<String, List<ItemStack>> pendingItems = new HashMap<>(); // Jogador -> itens pendentes
    public static File auctionFile;
    public static YamlConfiguration auctionConfig;
    public static final int ITEMS_PER_PAGE = 21; // 21 slots para leilões
    public static final int[] AUCTION_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    public Leilao() {
        super("leilao");
        auctionFile = new File(Main.get().getDataFolder(), "auctions.yml");
        auctionConfig = YamlConfiguration.loadConfiguration(auctionFile);
        
        loadAuctions();
        startAuctionChecker();
        startPendingItemChecker();
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            openAuctionMenu(player, 0);
            return;
        }

        if (args[0].equalsIgnoreCase("vender")) {
            if (args.length < 2) {
                player.sendMessage("§cUso: /leilao vender <preço inicial>");
                return;
            }

            try {
                double startingBid = Double.parseDouble(args[1]);
                if (startingBid <= 0) {
                    player.sendMessage("§cO preço inicial deve ser maior que 0!");
                    return;
                }

                ItemStack item = player.getInventory().getItemInHand();
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage("§cVocê precisa segurar um item para leiloar!");
                    return;
                }

                ItemMeta meta = item.getItemMeta();
                String itemName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
                Auction auction = new Auction(player.getName(), item.clone(), startingBid, itemName);
                auctions.add(auction);
                player.getInventory().setItemInHand(null);
                saveAuctions();
                Bukkit.broadcastMessage("§eNovo leilão iniciado por §f" + player.getName() + "§e: §f" + itemName + " §epor §a$" + startingBid);
                return;
            } catch (NumberFormatException e) {
                player.sendMessage("§cPor favor, insira um preço válido!");
            }
        } else if (args[0].equalsIgnoreCase("lance")) {
            if (args.length < 3) {
                player.sendMessage("§cUso: /leilao lance <índice> <valor>");
                return;
            }

            try {
                int index = Integer.parseInt(args[1]) - 1;
                double bid = Double.parseDouble(args[2]);
                if (index < 0 || index >= auctions.size()) {
                    player.sendMessage("§cÍndice de leilão inválido!");
                    return;
                }

                Auction auction = auctions.get(index);
                if (System.currentTimeMillis() >= auction.endTime) {
                    player.sendMessage("§cEste leilão já expirou!");
                    return;
                }
                
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

                if (auction.highestBidder != null) {
                    economy.depositPlayer(Bukkit.getOfflinePlayer(auction.highestBidder), auction.currentBid);
                }

                economy.withdrawPlayer(player, bid);
                auction.bidderAmounts.put(player.getName(), bid); // Armazena o lance do jogador
                auction.currentBid = bid;
                auction.highestBidder = player.getName();
                Bukkit.broadcastMessage("§eNovo lance de §f" + player.getName() + " §epor §a$" + bid + " §eno item §f" + auction.itemName);
                saveAuctions();
            } catch (NumberFormatException e) {
                player.sendMessage("§cPor favor, insira um índice e valor válidos!");
            }
        } else {
            player.sendMessage("§cUso: /leilao [vender <preço> | lance <índice> <valor>]");
        }
    }

    public static void openAuctionMenu(Player player, int page) {
        int totalPages = (int) Math.ceil((double) auctions.size() / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getName(), page);

        Inventory inv = Bukkit.createInventory(null, 54, "Leilão - Página " + (page + 1));

        // Adiciona item de informação
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§eInformações");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§eComo usar o leilão:");
        infoLore.add("");
        infoLore.add("§fPara vender:");
        infoLore.add("§7- Segure um item");
        infoLore.add("§7- Use §f/leilao vender <preço>");
        infoLore.add("");
        infoLore.add("§fPara dar lance:");
        infoLore.add("§7- Clique no item neste menu");
        infoLore.add("§7- Digite o valor no chat");
        infoLore.add("");
        infoLore.add("§cO valor será debitado!");
        infoLore.add("§cReembolsado se não vencer.");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inv.setItem(4, infoItem);

        // Verifica se não há leilões
        if (auctions.isEmpty()) {
            ItemStack noAuctionItem = new ItemStack(Material.WEB);
            ItemMeta noAuctionMeta = noAuctionItem.getItemMeta();
            noAuctionMeta.setDisplayName("§cNenhum Leilão Ativo");
            List<String> noAuctionLore = new ArrayList<>();
            noAuctionLore.add("§7No momento, não há");
            noAuctionLore.add("§7leilões em andamento.");
            noAuctionLore.add("");
            noAuctionLore.add("§7Crie um com:");
            noAuctionLore.add("§f/leilao vender <preço>");
            noAuctionMeta.setLore(noAuctionLore);
            noAuctionItem.setItemMeta(noAuctionMeta);
            inv.setItem(22, noAuctionItem);
        } else {
            // Adiciona itens do leilão nos slots especificados
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, auctions.size());
            int slotIndex = 0;

            for (int i = startIndex; i < endIndex && slotIndex < AUCTION_SLOTS.length; i++) {
                Auction auction = auctions.get(i);
                ItemStack item = auction.item.clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add("§fVendedor: §e" + auction.seller);
                    lore.add("§fLance Atual: §a$" + auction.currentBid);
                    lore.add("§fMaior Lanceador: §e" + (auction.highestBidder != null ? auction.highestBidder : "Nenhum"));
                    lore.add("§fTempo Restante: §e" + getTimeRemaining(auction.endTime));
                    lore.add("");
                    lore.add("§7Clique para dar lance!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(AUCTION_SLOTS[slotIndex++], item);
            }
        }

        // Adiciona botões de navegação
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§ePágina Anterior");
            prev.setItemMeta(prevMeta);
            inv.setItem(18, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§ePróxima Página");
            next.setItemMeta(nextMeta);
            inv.setItem(26, next);
        }

        player.openInventory(inv);
    }

    private void startAuctionChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                List<Auction> toRemove = new ArrayList<>();
                for (Auction auction : auctions) {
                    if (now >= auction.endTime) {
                        if (auction.highestBidder != null) {
                            Player winner = Bukkit.getPlayer(auction.highestBidder);
                            if (winner != null && canAddToInventory(winner, auction.item)) {
                                winner.getInventory().addItem(auction.item);
                                winner.sendMessage("§eVocê venceu o leilão de §f" + auction.itemName + " §epor §a$" + auction.currentBid + "!");
                            } else {
                                addPendingItem(auction.highestBidder, auction.item, "§eVocê venceu o leilão de §f" + auction.itemName + " §epor §a$" + auction.currentBid + "!");
                            }
                            Player seller = Bukkit.getPlayer(auction.seller);
                            if (seller != null) {
                                economy.depositPlayer(seller, auction.currentBid);
                                seller.sendMessage("§eSeu item §f" + auction.itemName + " §efoi vendido por §a$" + auction.currentBid + "!");
                            }
                            // Reembolsa jogadores que não venceram
                            for (Map.Entry<String, Double> entry : auction.bidderAmounts.entrySet()) {
                                String bidder = entry.getKey();
                                double amount = entry.getValue();
                                if (!bidder.equals(auction.highestBidder)) {
                                    economy.depositPlayer(Bukkit.getOfflinePlayer(bidder), amount);
                                    Player bidderPlayer = Bukkit.getPlayer(bidder);
                                    if (bidderPlayer != null) {
                                        bidderPlayer.sendMessage("§eVocê não venceu o leilão de §f" + auction.itemName + ". §a$" + amount + " §efoi reembolsado!");
                                    }
                                }
                            }
                        } else {
                            Player seller = Bukkit.getPlayer(auction.seller);
                            if (seller != null && canAddToInventory(seller, auction.item)) {
                                seller.getInventory().addItem(auction.item);
                                seller.sendMessage("§eSeu item §f" + auction.itemName + " §enão foi vendido e foi devolvido!");
                            } else {
                                addPendingItem(auction.seller, auction.item, "§eSeu item §f" + auction.itemName + " §enão foi vendido e foi devolvido!");
                            }
                        }
                        toRemove.add(auction);
                    }
                }
                auctions.removeAll(toRemove);
                if (!toRemove.isEmpty()) {
                	saveAuctions();
                }
            }
        }.runTaskTimerAsynchronously(Main.get(), 0L, 200L); // Verifica a cada 10 segundos
    }

    private void startPendingItemChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String playerName : new ArrayList<>(pendingItems.keySet())) {
                    Player player = Bukkit.getPlayer(playerName);
                    if (player != null) {
                        deliverPendingItems(player);
                    }
                }
            }
        }.runTaskTimer(Main.get(), 0L, 6000L); // Verifica a cada 5 minutos
    }

    private static boolean canAddToInventory(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int remaining = item.getAmount();
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem == null) {
                return true; // Slot vazio
            }
            if (slotItem.isSimilar(item) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                remaining -= (slotItem.getMaxStackSize() - slotItem.getAmount());
                if (remaining <= 0) return true; // Pode empilhar
            }
        }
        return false;
    }

    public static void deliverPendingItems(Player player) {
        List<ItemStack> items = pendingItems.get(player.getName());
        if (items == null || items.isEmpty()) return;

        List<ItemStack> delivered = new ArrayList<>();
        for (ItemStack item : new ArrayList<>(items)) {
            if (canAddToInventory(player, item)) {
                player.getInventory().addItem(item);
                player.sendMessage("§eItem entregue do leilão: §f" + (item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name()));
                delivered.add(item);
            }
        }

        items.removeAll(delivered);
        if (items.isEmpty()) {
            pendingItems.remove(player.getName());
        } else {
            player.sendMessage("§cAlguns itens não foram entregues devido a falta de espaço no inventário. Tente novamente mais tarde!");
        }
    }

    private static String getTimeRemaining(long endTime) {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        if (remaining <= 0) {
            return "Expirado";
        }
        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}