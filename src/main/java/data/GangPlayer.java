package data;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Cory on 19/04/2017.
 */
public class GangPlayer {

    private final UUID uuid;
    private String gang = "";
    private int kills = 0, deaths = 0, tokens = 0;
    private String rank = "";
    private boolean op = false;

    public GangPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public int getKills() {
        return this.kills;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public int getTokens() {
        return this.tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public int getKDR() {

        return this.deaths == 0 ? this.kills : (this.kills / this.deaths);
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public void setRank(String rank) {
        this.rank = ChatColor.translateAlternateColorCodes('&', rank);
    }

    public void setGang(String gang) {
        this.gang = gang;
    }

    public String getGang() {
        return this.gang;
    }

    public void setOp(boolean b) {
        op = b;
    }

    public boolean isOp() {
        return this.op;
    }

    public static GangPlayer wrap(Player p) {
        return new GangPlayer(p.getUniqueId());
    }

}
