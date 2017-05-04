package data;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.UUID;

/**
 * Created by Cory on 19/04/2017.
 */
public class Gang {

    private final String name;
    private HashSet<GangPlayer> members;
    private Location home = null;
    private String tag;

    public Gang(String name, HashSet<GangPlayer> members, String tag, Location home) {
        this.name = name;
        this.members = members;
        this.tag = tag;
        this.home = home;
    }

    public String getName() {
        return this.name;
    }

    public HashSet<GangPlayer> getMembers() {
        return this.members;
    }

    public String getTag() {
        return this.tag;
    }

    public Location getHome() {
        return this.home;
    }
}
