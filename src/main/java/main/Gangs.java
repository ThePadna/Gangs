package main;

import data.Gang;
import data.GangPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import sql.SQLite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by Cory on 19/04/2017.
 */
public class Gangs extends JavaPlugin implements Listener {

    private final SQLite sqLite = new SQLite(this);
    private final HashSet<Gang> gangHashSet = new HashSet<Gang>();
    private final HashSet<GangPlayer> gangPlayerHashSet = new HashSet<GangPlayer>();
    private final String table_Gangs = "gangs", table_Players = "players";

    @Override
    public void onEnable() {
        sqLite.getSQLConnection();
        sqLite.load();
        this.getServer().getPluginManager().registerEvents(this, this);
        try {
            PreparedStatement preparedStatement = null;
            Connection connection = reCon();
            preparedStatement = connection.prepareStatement("SELECT * FROM " + table_Gangs);
            ResultSet resultSet = null;
            if (preparedStatement != null) {
                resultSet = preparedStatement.executeQuery();
                if(resultSet == null) Util.log("resultset null");
            }
            if (resultSet != null) {
                while (resultSet.next()) {
                    String name = resultSet.getString("gang");
                    String members = resultSet.getString("members");
                    String tag = resultSet.getString("tag");
                    String home = resultSet.getString("home");

                    Location l = null;
                    if(home != null) {
                        String split[] = home.split(",");
                        World world = Bukkit.getWorld(split[0]);
                        if(world != null) {
                            l = new Location(world, Integer.valueOf(split[1]), Integer.valueOf(split[2]), Integer.valueOf(split[3]));
                        }
                    }

                    HashSet<GangPlayer> membersList = new HashSet<GangPlayer>();
                    String[] membersSplit = members.split(",");
                    for(String s : membersSplit) {
                        GangPlayer gangPlayer = getGangPlayerFromUUID(UUID.fromString(s));
                        if(gangPlayer != null) {
                            membersList.add(gangPlayer);
                        }
                    }

                    Gang gang = new Gang(name, membersList, tag, l);
                    gangHashSet.add(gang);
                }
            }
            preparedStatement.close();
            connection.close();
        } catch(SQLException e) {
            Util.log("SQL Exception while performing Gangs HashSet fill:");
            e.printStackTrace();
        }
    }

    private GangPlayer getGangPlayerFromUUID(UUID uuid) {
        for(GangPlayer gangPlayer : this.gangPlayerHashSet) {
            if(gangPlayer.getUUID() == uuid) return gangPlayer;
        }
        return null;
    }

    @Override
    public void onDisable() {
        flushData();
    }

    private void flushData() {
        Bukkit.broadcastMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "Gangs" + ChatColor.GRAY + "] " + "[" + ChatColor.BLUE + "SQL" + ChatColor.GRAY + "] " + ChatColor.WHITE + "Flushing data to local DB...");
        try {
            PreparedStatement preparedStatement = null;
            Connection connection = reCon();
            for(Gang g : this.gangHashSet) {
                preparedStatement = connection.prepareStatement("INSERT INTO " + table_Gangs + " (gang,members,tag,home) VALUES(?,?,?,?)");
                preparedStatement.setString(1, g.getName());
                preparedStatement.setString(2, serializeMembers(g.getMembers()));
                preparedStatement.setString(3, g.getTag());
                if(g.getHome() != null) preparedStatement.setString(4, serializeLocation(g.getHome()));
                else preparedStatement.setString(4, null);
                preparedStatement.executeUpdate();
            }
            for(GangPlayer gp : this.gangPlayerHashSet) {
                preparedStatement = connection.prepareStatement("INSERT INTO " + table_Players + " (player,gang,kills,deaths,tokens) VALUES(?,?,?,?,?)");
                preparedStatement.setString(1, gp.getUUID().toString());
                preparedStatement.setString(2, gp.getGang());
                preparedStatement.setInt(3, gp.getKills());
                preparedStatement.setInt(4, gp.getDeaths());
                preparedStatement.setInt(5, gp.getTokens());
                preparedStatement.executeUpdate();
            }
            connection.close();
        } catch(SQLException e) {
            Util.log("SQL Exception when flushing RAM data to DB.");
        }
    }

    private String serializeMembers(HashSet<GangPlayer> members) {
        StringBuilder sb = new StringBuilder();
        for(GangPlayer gangPlayer : members) {
            sb.append(gangPlayer.getUUID() + ",");
        }
        return sb.substring(sb.length()-1).toString();
    }

    private String serializeLocation(Location l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.getWorld().getName() + ",");
        sb.append(l.getBlockX() + ",");
        sb.append(l.getBlockY() + ",");
        sb.append(l.getBlockZ());
        return sb.toString();
    }

    public Connection reCon() {
        return sqLite.getSQLConnection();
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String cmdLabel, String[] args) {
        if(cmdLabel.equalsIgnoreCase("gangs") || cmdLabel.equalsIgnoreCase("gang")) {
            if(args.length == 0) {
                this.correct(s, CorrectionMessage.GENERAL);
                return false;
            }
            String section = args[0];
            if(section.equalsIgnoreCase("create")) {
                if(args.length > 2 || args.length < 2) {
                    this.correct(s, CorrectionMessage.CREATE);
                } else if(!(s instanceof Player)) {
                    this.correct(s, CorrectionMessage.CONSOLE);
                } else {
                    if(gangExists(args[0]) != null) {
                        s.sendMessage(ChatColor.RED + "Error: Gang already exists with this name.");
                        return false;
                    }
                    Player p = (Player) s;
                    HashSet<GangPlayer> players = new HashSet<GangPlayer>();
                    GangPlayer gangPlayer = getGangPlayerFromUUID(p.getUniqueId());
                    if(gangPlayer == null) {
                        s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + p.getUniqueId());
                        return false;
                    }
                    if(gangPlayer.getGang().length() > 0) {
                        s.sendMessage(ChatColor.RED + "Error: You are already in a Gang.");
                        return false;
                    }
                    gangPlayer.setRank("&7[&4Owner&7]");
                    gangPlayer.setOp(true);
                    gangPlayer.setGang(args[1]);
                    players.add(gangPlayer);
                    Gang gang = new Gang(args[1], players, "", null);
                    this.gangHashSet.add(gang);
                }
            } else if(section.equalsIgnoreCase("disband")) {
                if(args.length > 1) {
                    this.correct(s, CorrectionMessage.DISBAND);
                    return false;
                }
                Player p = (Player) s;
                GangPlayer gangPlayer = getGangPlayerFromUUID(p.getUniqueId());
                if(gangPlayer == null) {
                    s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + p.getUniqueId());
                    return false;
                } else {
                    if(gangPlayer.getGang().length() == 0) {
                        s.sendMessage(ChatColor.RED + "Error: You must be Operator in a gang to perform this command.");
                    } else if(!gangPlayer.isOp()) {
                        s.sendMessage(ChatColor.RED + "Error: You must have OP privileges within your gang to perform this command.");
                    } else {
                        s.sendMessage(ChatColor.BLUE + "Disbanded Gang " + ChatColor.AQUA + gangPlayer.getGang());
                        this.disband(this.gangExists(gangPlayer.getGang()));
                    }
                }
            } else if(section.equalsIgnoreCase("lb")) {
                for(Gang gang : this.gangHashSet) {
                    s.sendMessage(gang.getName() + "\n");
                }
            } else if(section.equalsIgnoreCase("kick")) {
                if(args.length > 2 || args.length < 2) {
                    this.correct(s, CorrectionMessage.KICK);
                } else if(args.length == 2) {
                    String player = args[1];
                    if(s.getName().equalsIgnoreCase(player)) {
                        s.sendMessage(ChatColor.RED + "Error: You cannot kick yourself, silly.");
                        return false;
                    }
                    Player player_Obj = Bukkit.getPlayer(player);
                    if(player_Obj == null) {
                        s.sendMessage(ChatColor.RED + "Error: No such player.");
                        return false;
                    }
                    GangPlayer gangPlayer = getGangPlayerFromUUID(player_Obj.getUniqueId());
                    if(gangPlayer == null) {
                        s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + player_Obj.getUniqueId());
                        return false;
                    }
                    Gang gang = gangExists(gangPlayer.getGang());
                    if(gang == null) {
                        s.sendMessage(ChatColor.RED + "Player isn't in a gang.");
                        return false;
                    }
                    Player p = (Player) s;
                    GangPlayer gangPlayerCommandSender = getGangPlayerFromUUID(p.getUniqueId());
                    if(gangPlayer == null) {
                        s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + p.getUniqueId());
                        return false;
                    }

                    if(gangPlayerCommandSender.getGang().equalsIgnoreCase(gangPlayer.getGang())) {
                        if(gangPlayer.isOp()) {
                            s.sendMessage(ChatColor.RED + "Error: You may not kick an OP gang member.");
                            return false;
                        } else if(!gangPlayerCommandSender.isOp()) {
                            s.sendMessage(ChatColor.RED + "Error: You must be OP within your gang to perform this command.");
                            return false;
                        } else {
                            s.sendMessage(ChatColor.RED + "Kicked " + ChatColor.GOLD + player_Obj.getName() + ChatColor.RED + " from your gang.");
                            if(player_Obj.isOnline()) player_Obj.sendMessage(ChatColor.RED + "You have been kicked from your gang.");
                            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mail send " + player_Obj.getName() + " You have been kicked from your clan.");
                            gangPlayer.setRank("");
                            gangPlayer.setGang("");
                            gang.getMembers().remove(gangPlayer);
                        }
                    } else {
                        s.sendMessage(ChatColor.RED + "Error: Player isn't within your gang.");
                    }
                }
            } else if(section.equalsIgnoreCase("rank")) {

            } else if(section.equalsIgnoreCase("stats")) {
                if(args.length > 2) {
                    this.correct(s, CorrectionMessage.STATS);
                } else if(args.length == 2) {
                    String player = args[1];
                    Player player_Obj = Bukkit.getPlayer(player);
                    if(player_Obj == null) {
                        s.sendMessage(ChatColor.RED + "Error: No such player.");
                        return false;
                    }
                    GangPlayer gangPlayer = getGangPlayerFromUUID(player_Obj.getUniqueId());
                    if(gangPlayer == null) {
                        s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + player_Obj.getUniqueId());
                        return false;
                    }
                    s.sendMessage(this.readStats(gangPlayer));
                } else if(args.length == 1) {
                    Player p = (Player) s;
                    GangPlayer gangPlayer = getGangPlayerFromUUID(p.getUniqueId());
                    if(gangPlayer == null) {
                        s.sendMessage(ChatColor.RED + "Error (Contact an Administrator): Can't find GangPlayer object for UUID: " + p.getUniqueId());
                        return false;
                    }
                    s.sendMessage(this.readStats(gangPlayer));
                }
            } else if(section.equalsIgnoreCase("invite")) {

            }
        }
        return false;
    }

    private void correct(CommandSender s, CorrectionMessage correctionMessage) {
        switch(correctionMessage) {
            case CREATE:
                s.sendMessage(ChatColor.RED + "Error: Invalid Arguments.\n " + ChatColor.BLUE + "Try: /gangs create <GangName>");
            break;
            case CONSOLE:
                s.sendMessage(ChatColor.RED + "Error: Cannot use this command in Console.");
                break;
            case LB:
                s.sendMessage(ChatColor.RED + "Error: Invalid Arguments.\n " + ChatColor.BLUE + "Try: /gangs lb <[OPTIONAL]PAGE_NUMBER>");
                break;
            case GENERAL:
                s.sendMessage( "========== [ " + ChatColor.BLUE + "Captivity Gangs" + ChatColor.RESET + " ] ==========\n"
                        + ChatColor.BLUE + "/gangs create " + ChatColor.AQUA + "<GangName> " + ChatColor.BLUE + "- Create a gang.\n"
                        + ChatColor.BLUE + "/gangs lb " + ChatColor.AQUA + "[OPTIONAL]<PAGE_NUMBER> <player|gang>" + ChatColor.BLUE + "- Gang leaderboards.\n"
                        + ChatColor.BLUE + "/gangs invite " + ChatColor.AQUA + "<Player> " + ChatColor.BLUE + "- Invite a player to your gang.\n"
                        + ChatColor.BLUE + "/gangs rank " + ChatColor.AQUA + "<Player> <Rank> " + ChatColor.BLUE + "- Rank a player in your gang chat.\n"
                        + ChatColor.BLUE + "/gangs kick " + ChatColor.AQUA + "<Player> " + ChatColor.BLUE + "- Kick a player from your gang.\n"
                        + ChatColor.BLUE + "/gangs stats " + ChatColor.AQUA + "[OPTIONAL]<Player> " + ChatColor.BLUE + "- Check stats for an individual.\n"
                        + ChatColor.BLUE + "/gangs store " + ChatColor.BLUE + "- View the token store.\n"
                        + ChatColor.BLUE + "/gangs disband " + ChatColor.BLUE + "- Disband your gang.\n"
                        + ChatColor.BLUE + "/gangs tag " + ChatColor.AQUA + "<tag> " + ChatColor.BLUE + "- Give your gang a tag in chat.\n"
                        + ChatColor.BLUE + "/gangs chat|c " + ChatColor.AQUA + "[OPTIONAL]<Message>" + ChatColor.BLUE + "- Speak in gang chat, or toggle gang chat.\n"
                        + ChatColor.BLUE + "/gangs op " + ChatColor.AQUA + "<Player>" + ChatColor.BLUE + "- Give a player OP privileges within your gang.\n"
                        + ChatColor.RESET + "========== [ " + ChatColor.BLUE + "Captivity Gangs" + ChatColor.RESET + " ] ==========");
                break;
            case DISBAND:
                s.sendMessage(ChatColor.RED + "Error: Invalid Arguments.\n " + ChatColor.BLUE + "Try: /gangs disband");
                break;
            case STATS:
                s.sendMessage(ChatColor.RED + "Error: Invalid Arguments.\n " + ChatColor.BLUE + "Try: /gangs stats [OPTIONAL]<Player>");
                break;
            case KICK:
                s.sendMessage(ChatColor.RED + "Error: Invalid Arguments.\n " + ChatColor.BLUE + "Try: /gangs kick <Player>");
                break;
        }
    }

    private enum CorrectionMessage {
        CREATE, DISBAND, LB, KICK, RANK, STATS, INVITE, CONSOLE, GENERAL;
    }

    private Gang gangExists(String gangName) {
        for(Gang g : this.gangHashSet) {
            if(g.getName().equalsIgnoreCase(gangName)) {
                return g;
            }
        }
        return null;
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(getGangPlayerFromUUID(e.getPlayer().getUniqueId()) == null) {
            this.gangPlayerHashSet.add(GangPlayer.wrap(e.getPlayer()));
        }
    }

    private void disband(Gang gang) {
        for(GangPlayer gangPlayer : gang.getMembers()) {
            gangPlayer.setOp(false);
            gangPlayer.setRank("");
        }
        this.gangPlayerHashSet.remove(gang);
        gang = null;
    }

    private String readStats(GangPlayer gangPlayer) {
        Player p = Bukkit.getPlayer(gangPlayer.getUUID());
        return "========== [ " + ChatColor.BLUE + p.getDisplayName() + ChatColor.RESET + " ] ==========\n"
                + ChatColor.BLUE + "Kills: " + ChatColor.AQUA + gangPlayer.getKills() + "\n"
                + ChatColor.BLUE + "Deaths: " + ChatColor.AQUA + gangPlayer.getDeaths() + "\n"
                + ChatColor.BLUE + "KDR: " + ChatColor.AQUA + gangPlayer.getKDR() + "\n"
                + ChatColor.BLUE + "Tokens: " + ChatColor.AQUA + gangPlayer.getTokens() + "\n"
                + "========== [ " + ChatColor.BLUE + p.getDisplayName() + ChatColor.RESET + " ] ==========\n";
    }
}
