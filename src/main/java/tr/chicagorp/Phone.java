package tr.chicagorp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.*;

/** Telefon: 6 bolumlu menu + yol tarifi (mor isik sutunu). */
public class Phone implements Listener {
    private final ChicagoRP p;
    private final Map<UUID, Location> nav = new HashMap<>();
    private final Map<UUID, String> navName = new HashMap<>();
    private final Map<UUID, Long> policeCd = new HashMap<>();

    public Phone(ChicagoRP p) {
        this.p = p;
        Bukkit.getScheduler().runTaskTimer(p, this::navTick, 10L, 10L);
    }

    public void give(Player pl) {
        if (U.count(pl, "telefon") > 0) return;
        ItemStack it = U.custom(p.getConfig().getString("phone-ia", ""), U.item(Material.IRON_NUGGET, "&bTelefon &7(sağ tık)", "telefon"));
        pl.getInventory().addItem(it);
    }

    @EventHandler public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (U.hasTag(e.getItem(), "telefon")) { e.setCancelled(true); open(e.getPlayer()); }
    }

    public void open(Player pl) {
        Gui g = new Gui("telefon", 27, "&8Telefon");
        g.put(10, U.icon(Material.COMPASS, "&aŞehir", "&7Yer ve yol tarifi"), "page:sehir");
        g.put(11, U.icon(Material.EMERALD, "&aİşler & Para", "&7Meslek seç, bakiye"), "page:isler");
        g.put(12, U.icon(Material.MINECART, "&aAraçlar", "&7Garajın"), "page:arac");
        g.put(14, U.icon(Material.SHIELD, "&aÇete & Güvenlik", "&7PvP, çete, polis çağır"), "page:cete");
        g.put(15, U.icon(Material.PLAYER_HEAD, "&aSosyal", "&7Oyuncular"), "page:sosyal");
        g.put(16, U.icon(Material.NETHER_STAR, "&aÖdüller & Seviye", "&7Günlük ödül"), "page:oduller");
        g.put(22, U.icon(Material.GOLD_INGOT, "&6Bakiye: &f" + (long) p.data.money(pl.getUniqueId()) + " TL"), null);
        pl.openInventory(g.inv);
    }

    private Gui sub(String page, int size, String title) {
        Gui g = new Gui(page, size, title);
        g.put(size - 1, U.icon(Material.ARROW, "&7Geri"), "back");
        return g;
    }

    private void page(Player pl, String pg) {
        UUID u = pl.getUniqueId();
        switch (pg) {
            case "sehir" -> {
                Gui g = sub("telefon", 36, "&8Şehir");
                var s = p.getConfig().getConfigurationSection("places");
                int i = 0;
                if (s != null) for (String id : s.getKeys(false)) { if (i >= 30) break; g.put(i++, U.icon(Material.MAP, "&e" + s.getString(id + ".name", id), "&7Tıkla: yol tarifi"), "nav:" + id); }
                g.put(31, U.icon(Material.BARRIER, "&cYol tarifini kapat"), "navoff");
                pl.openInventory(g.inv);
            }
            case "isler" -> {
                Gui g = sub("telefon", 36, "&8İşler & Para");
                int i = 0;
                for (String j : JobManager.ALL) g.put(i++, U.icon(j.equals(p.data.job(u)) ? Material.LIME_DYE : Material.PAPER, "&e" + j, "&7Tıkla: bu mesleği seç"), "job:" + j);
                g.put(30, U.icon(Material.GOLD_INGOT, "&6Bakiye: &f" + (long) p.data.money(u) + " TL"), null);
                pl.openInventory(g.inv);
            }
            case "arac" -> p.vehicles.openGarage(pl);
            case "cete" -> {
                Gui g = sub("telefon", 27, "&8Çete & Güvenlik");
                g.put(10, U.icon(Material.LIME_CONCRETE, "&aPvP aç"), "pvp:ac");
                g.put(11, U.icon(Material.RED_CONCRETE, "&cPvP kapat", "&7Sadece spawn'da"), "pvp:kapat");
                g.put(12, U.icon(Material.IRON_SWORD, "&eÇete bilgisi", "&7" + p.gang.info(u)), "gang");
                g.put(14, U.icon(Material.BELL, "&9Polisi çağır", "&7Konumun polislere gider"), "polis");
                g.put(15, U.icon(Material.NAME_TAG, "&eKimliğim"), "kimlik");
                pl.openInventory(g.inv);
            }
            case "sosyal" -> {
                Gui g = sub("telefon", 54, "&8Sosyal");
                int i = 0;
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (i >= 45) break;
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta m = (SkullMeta) head.getItemMeta();
                    m.setOwningPlayer(o); m.displayName(U.L.deserialize("&e" + o.getName())); head.setItemMeta(m);
                    g.put(i++, head, "profil:" + o.getName());
                }
                pl.openInventory(g.inv);
            }
            case "oduller" -> {
                Gui g = sub("telefon", 27, "&8Ödüller & Seviye");
                g.put(11, U.icon(Material.EXPERIENCE_BOTTLE, "&bSeviye: &f" + p.data.level(u)), null);
                g.put(13, U.icon(Material.CHEST, "&6Günlük ödül", "&71500 TL + 100 XP", "&7Tıkla: al"), "daily");
                pl.openInventory(g.inv);
            }
        }
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Gui g) || !g.page.equals("telefon")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player pl)) return;
        String a = g.act.get(e.getRawSlot());
        if (a == null) return;
        UUID u = pl.getUniqueId();
        if (a.startsWith("page:")) page(pl, a.substring(5));
        else if (a.equals("back")) open(pl);
        else if (a.startsWith("job:")) { pl.performCommand("meslek " + a.substring(4)); page(pl, "isler"); }
        else if (a.startsWith("nav:")) {
            var s = p.getConfig().getConfigurationSection("places." + a.substring(4));
            var w = s == null ? null : Bukkit.getWorld(s.getString("world", "sehir"));
            if (w == null) return;
            nav.put(u, new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z")));
            navName.put(u, s.getString("name", a.substring(4)));
            pl.setCompassTarget(nav.get(u));
            pl.closeInventory();
            U.msg(pl, "Yol tarifi: &e" + navName.get(u) + " &f(mor ışık sütununu takip et)");
        }
        else if (a.equals("navoff")) { nav.remove(u); pl.closeInventory(); U.msg(pl, "Yol tarifi kapatıldı."); }
        else if (a.equals("pvp:ac")) { pl.closeInventory(); p.pvp.toggle(pl, new String[]{"ac"}); }
        else if (a.equals("pvp:kapat")) { pl.closeInventory(); p.pvp.toggle(pl, new String[]{"kapat"}); }
        else if (a.equals("gang")) { pl.closeInventory(); U.msg(pl, "Çete: " + p.gang.info(u) + " | /cete kur, davet, katil, ayril, baris"); }
        else if (a.equals("kimlik")) { pl.closeInventory(); pl.performCommand("kimlik"); }
        else if (a.startsWith("profil:")) { pl.closeInventory(); pl.performCommand("profil " + a.substring(7)); }
        else if (a.equals("polis")) { pl.closeInventory(); callPolice(pl); }
        else if (a.equals("daily")) daily(pl);
    }

    private void callPolice(Player pl) {
        long now = System.currentTimeMillis();
        if (policeCd.getOrDefault(pl.getUniqueId(), 0L) + 60000L > now) { U.msg(pl, "Az önce çağırdın, bekle."); return; }
        policeCd.put(pl.getUniqueId(), now);
        int n = 0;
        Location l = pl.getLocation();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!p.police.isPolice(o)) continue;
            n++;
            U.msg(o, "&c[ACİL] &f" + pl.getName() + " yardım istiyor: " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + " (yol tarifi açıldı)");
            nav.put(o.getUniqueId(), l.clone()); navName.put(o.getUniqueId(), "Acil çağrı");
        }
        U.msg(pl, n == 0 ? "Şu an aktif polis yok." : "Polis çağrıldı (" + n + " görevli).");
    }

    private void daily(Player pl) {
        UUID u = pl.getUniqueId();
        long last = ((Number) p.data.get(u, "daily", 0L)).longValue(), now = System.currentTimeMillis();
        if (last + 24L * 3600_000L > now) { U.msg(pl, "Günlük ödülü zaten aldın."); return; }
        p.data.set(u, "daily", now); p.data.add(u, 1500); p.data.addXp(u, 100);
        U.msg(pl, "&a+1500 TL, +100 XP");
        pl.closeInventory();
    }

    private void navTick() {
        for (UUID u : new ArrayList<>(nav.keySet())) {
            Player pl = Bukkit.getPlayer(u);
            Location t = nav.get(u);
            if (pl == null || !pl.getWorld().equals(t.getWorld())) { if (pl == null) nav.remove(u); continue; }
            double d = pl.getLocation().distance(t);
            if (d < 5) { nav.remove(u); U.msg(pl, "Hedefe vardın."); continue; }
            pl.setCompassTarget(t);
            pl.sendActionBar(Component.text("Hedef: " + navName.getOrDefault(u, "?") + " — " + (int) d + " m"));
            if (d < 48) for (int y = 0; y < 40; y += 2)
                pl.spawnParticle(Particle.DUST, t.getX(), t.getY() + y, t.getZ(), 1, new Particle.DustOptions(Color.fromRGB(170, 40, 240), 2f));
        }
    }
}
