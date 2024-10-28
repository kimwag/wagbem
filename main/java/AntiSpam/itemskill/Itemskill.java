package AntiSpam.itemskill;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Itemskill extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private long COOLDOWN_TIME;
    private double DASH_POWER;
    private double DASH_UPWARD;
    private ItemStack abilityItem;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        config = getConfig();
        loadConfig();


        Objects.requireNonNull(this.getCommand("abilityitem")).setExecutor(this::onCommand);

        getLogger().info("ItemSkill 플러그인이 활성화되었습니다!");
    }

    private void loadConfig() {
        DASH_POWER = config.getDouble("settings.dash.power", 1.5);
        DASH_UPWARD = config.getDouble("settings.dash.upward", 0.2);
        COOLDOWN_TIME = config.getLong("settings.dash.cooldown", 10) * 1000; // 초 단위를 밀리초로 변환

        loadAbilityItem();
    }

    private void loadAbilityItem() {
        if (config.contains("abilityItem")) {
            abilityItem = config.getItemStack("abilityItem");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("itemskill.admin")) {
            player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }


        if (args.length == 0) {
            showHelp(player);
            return true;
        }


        switch (args[0].toLowerCase()) {
            case "설정":
                setAbilityItem(player);
                break;
            case "제거":
                removeAbilityItem(player);
                break;
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6[ ItemSkill 명령어 ]");
        player.sendMessage("§f/능력아이템 설정 §7- 손에 든 아이템을 능력 아이템으로 설정");
        player.sendMessage("§f/능력아이템 제거 §7- 능력 아이템 설정 제거");
    }

    private void setAbilityItem(Player player) {
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§c손에 아이템을 들고 있어야 합니다.");
            return;
        }

        abilityItem = handItem.clone();
        config.set("abilityItem", abilityItem);
        saveConfig();

        player.sendMessage("§a능력 아이템이 설정되었습니다!");
        player.sendMessage("§7설정된 아이템: §f" + handItem.getType().name());
    }

    private void removeAbilityItem(Player player) {
        if (abilityItem == null) {
            player.sendMessage("§c설정된 능력 아이템이 없습니다.");
            return;
        }

        String itemName = abilityItem.getType().name();
        config.set("abilityItem", null);
        abilityItem = null;
        saveConfig();

        player.sendMessage("§a능력 아이템이 제거되었습니다!");
        player.sendMessage("§7제거된 아이템: §f" + itemName);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking()) return;


        ItemStack helmet = player.getInventory().getHelmet();
        if (!isSimilarItem(helmet, abilityItem)) return;


        if (player.getLocation().subtract(0, 0.1, 0).getBlock().isPassable()) {// 쿨다운 확인
            if (isOnCooldown(player)) {
                player.sendMessage("§c쿨타임이 남아있습니다: " + getRemainingCooldown(player) + "초");
                return;
            }


            Vector direction = player.getLocation().getDirection();
            direction.multiply(DASH_POWER);
            direction.setY(DASH_UPWARD);
            player.setVelocity(direction);

            setCooldown(player);
            player.sendMessage("§a돌진!");
        }
    }

    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        return item1.getType() == item2.getType() &&
                Objects.equals(item1.getItemMeta(), item2.getItemMeta());
    }

    private boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) &&
                System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < COOLDOWN_TIME;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private long getRemainingCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        return Math.max(0, (COOLDOWN_TIME -
                (System.currentTimeMillis() - cooldowns.get(player.getUniqueId()))) / 1000);
    }
}
