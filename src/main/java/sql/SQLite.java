package sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import main.Gangs;


public class SQLite extends Database{
    public SQLite(Gangs instance){
        super(instance);
    }

    private final String gangsTableST = "CREATE TABLE IF NOT EXISTS gangs (" +
            "`gang` varchar(32) NOT NULL," + // This creates the different columns you will save data too. varchar(32) Is a string, int = integer
            "`members` varchar(32) NOT NULL," +
            "`tag` varchar(32) NOT NULL," +
            "`home` varchar(32)," +
            "PRIMARY KEY (`gang`)" +
            ");";
    private final String playerTableST = "CREATE TABLE IF NOT EXISTS players (" +
            "`player` varchar(32) NOT NULL," +
            "`gang` varchar(32) NOT NULL," +
            "`kills` INT(11) NOT NULL," +
            "`deaths` INT(11) NOT NULL," +
            "`tokens` INT(11) NOT NULL," +
            "PRIMARY KEY (`player`)" +
            ");";


    // SQL creation stuff, You can leave the blow stuff untouched.
    public Connection getSQLConnection() {
        plugin.getDataFolder().mkdirs();
        File dataFolder = new File(plugin.getDataFolder(), "gangs.db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "File write error: gangs.db");
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(gangsTableST);
            s.executeUpdate(playerTableST);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}
