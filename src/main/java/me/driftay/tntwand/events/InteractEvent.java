package me.driftay.tntwand.events;

import com.massivecraft.factions.*;
import com.massivecraft.factions.listeners.FactionsBlockListener;
import me.driftay.tntwand.hooks.HookManager;
import me.driftay.tntwand.hooks.impl.WorldGuardHook;
import me.driftay.tntwand.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static me.driftay.tntwand.utils.Utils.color;
import static me.driftay.tntwand.utils.Utils.config;

public class InteractEvent implements Listener {


    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInHand();
        ItemMeta im = itemInHand.getItemMeta();
        String displayname = color(Utils.config.getString("Item.Display-Name"));
        String successMessage = color(Utils.config.getString("SavageTnTWand.Success-Message"));
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(event.getPlayer());
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(block.getLocation()));

        Faction faction = fplayer.getFaction();

        if (player.getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }

        if (itemInHand.getType() != Material.GOLD_HOE
                || !im.hasDisplayName()
                || !im.getDisplayName().equalsIgnoreCase(displayname)) {
            return;
        }

        Chest chest = (Chest) block.getState();

        if (HookManager.getPluginMap().get("WorldGuard") != null) {
            WorldGuardHook wgHook = ((WorldGuardHook) HookManager.getPluginMap().get("WorldGuard"));
            if (!wgHook.canBuild(player, block)) {
                player.sendMessage(color(config.getString("Valid-Chunks.Deny-Message").replace("%faction%", otherFaction.getTag())));
                return;
            }
        }
        if (!FactionsBlockListener.playerCanBuildDestroyBlock(player, block.getLocation(), "build", true)) {
            event.setCancelled(true);
            player.sendMessage(color(config.getString("Valid-Chunks.Deny-Message").replace("%faction%", otherFaction.getTag())));
            return;
        }

        event.setCancelled(true);

        int tntSpaceLeft = faction.getTntBankLimit() - faction.getTnt();
        if (tntSpaceLeft <= 0) {
            player.sendMessage(color(config.getString("SavageTnTWand.Bank-Full")));
            return;
        }

        int tntcount = 0;
        Inventory inventory = chest.getInventory();
        for (int i = 0; i < inventory.getSize(); ++i) {
            ItemStack tnt = inventory.getItem(i);
            if (tnt != null && tnt.getType() == Material.TNT) {
                int space = tntSpaceLeft - tnt.getAmount();
                if (space >= 0) {
                    tntcount += tnt.getAmount();
                    tntSpaceLeft = space;
                    inventory.removeItem(tnt);
                } else {
                    tntcount += tntSpaceLeft;
                    tnt.setAmount(tnt.getAmount() - tntSpaceLeft);
                    break;
                }
            }
        }

        if (tntcount > 0) {
            successMessage = successMessage.replace("%amount%", Integer.toString(tntcount));
            faction.addTnt(tntcount);
            player.sendMessage(successMessage);
        }

    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        Action action = e.getAction();
        Player player = e.getPlayer();
        ItemStack itemInHand = player.getItemInHand();
        ItemMeta im = itemInHand.getItemMeta();
        String displayname = color(Utils.config.getString("Item.Display-Name"));
        Block block = e.getClickedBlock();
        if (block == null) return;

        if (itemInHand.getType().equals(Material.GOLD_HOE)) {
            if (im.getDisplayName().equalsIgnoreCase(displayname)) {
                if (action.equals(Action.RIGHT_CLICK_BLOCK)) {
                    if (block.getType() == Material.GRASS || block.getType() == Material.DIRT) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}

