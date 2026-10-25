package tr.chicagorp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class JobManager implements Listener {
    private final ChicagoRP p;
    public static final List<String> ALL = List.of("ciftci", "kimyager", "torbaci", "demirci", "kargocu",
            "silah_ustasi", "balikci", "mekanik", "taksici", "durumcu", "polis", "kacakci", "soyguncu");

    public JobManager(ChicagoRP p) { this.p = p; }

    public void command(Player pl, String[] a) {
        if (a.length == 0) { U.msg(pl, "Meslekler: " + String.join(", ", ALL) + " | Mevcut: " + p.data.job(pl.getUniqueId())); return; }
        String j = a[0].toLowerCase();
        if (!ALL.contains(j) && !j.equals("yok")) { U.msg(pl, "Boyle bir meslek yok."); return; }
        if (j.equals("polis") && !pl.hasPermission("rp.job.polis")) { U.msg(pl, "Polis icin destek talebi acip mulakata girmelisin."); return; }
        p.data.set(pl.getUniqueId(), "job", j);
        U.msg(pl, "Meslegin: " + j);
    }

    @EventHandler public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player pl = e.getPlayer();
        if (!p.data.job(pl.getUniqueId()).equals("balikci")) return;
        var c = p.getConfig().getConfigurationSection("fishing");
        int pay = ThreadLocalRandom.current().nextInt(c.getInt("min"), c.getInt("max") + 1);
        p.data.add(pl.getUniqueId(), pay); p.data.addXp(pl.getUniqueId(), 20);
        U.msg(pl, "Balik sattin: &a+" + pay + " TL");
    }
}
