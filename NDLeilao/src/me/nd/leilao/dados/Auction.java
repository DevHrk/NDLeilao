package me.nd.leilao.dados;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import static me.nd.leilao.cmd.Leilao.auctionFile;
import static me.nd.leilao.cmd.Leilao.auctionConfig;
import static me.nd.leilao.cmd.Leilao.auctions;

public class Auction {
	public String seller;
    public ItemStack item;
    public double currentBid;
    public String highestBidder;
    public long endTime;
    public String itemName;
    public Map<String, Double> bidderAmounts; // Jogador -> valor do lance

    public Auction(String seller, ItemStack item, double startingBid, String itemName) {
        this.seller = seller;
        this.item = item;
        this.currentBid = startingBid;
        this.highestBidder = null;
        this.endTime = System.currentTimeMillis() + 3600000L; // 1 minuto
        this.itemName = itemName;
        this.bidderAmounts = new HashMap<>();
    }
    
    @SuppressWarnings("unused")
	public static void loadAuctions() {
        if (!auctionFile.exists()) return;
        for (String key : auctionConfig.getKeys(false)) {
            String seller = auctionConfig.getString(key + ".seller");
            ItemStack item = auctionConfig.getItemStack(key + ".item");
            double currentBid = auctionConfig.getDouble(key + ".currentBid");
            
			String highestBidder = auctionConfig.getString(key + ".highestBidder");
            long endTime = auctionConfig.getLong(key + ".endTime");
            String itemName = auctionConfig.getString(key + ".itemName");
            // Verifica se os dados essenciais estão presentes
            if (seller == null || item == null || itemName == null) {
                continue;
            }
            auctions.add(new Auction(seller, item, currentBid, itemName) {
                {
                    this.highestBidder = highestBidder;
                    this.endTime = endTime;
                }
            });
        }
    }

    public static void saveAuctions() {
        auctionConfig.getKeys(false).forEach(key -> auctionConfig.set(key, null));
        for (int i = 0; i < auctions.size(); i++) {
            Auction auction = auctions.get(i);
            String key = "auction." + i;
            auctionConfig.set(key + ".seller", auction.seller);
            auctionConfig.set(key + ".item", auction.item);
            auctionConfig.set(key + ".currentBid", auction.currentBid);
            if (auction.highestBidder != null) {
                auctionConfig.set(key + ".highestBidder", auction.highestBidder);
            }
            auctionConfig.set(key + ".endTime", auction.endTime);
            auctionConfig.set(key + ".itemName", auction.itemName);
        }
        try {
            auctionConfig.save(auctionFile);
        } catch (Exception e) {
        }
    }
}
