package ovh.quiquelhappy.mcplugins.purestats.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import ovh.quiquelhappy.mcplugins.purestats.listener.listener.join;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;

public class main extends Plugin {

    public static String sqlconn = "";
    ProxyServer proxy = ProxyServer.getInstance();

    @Override
    public void onEnable() {
        super.onEnable();

        System.out.println("  _____                 _____ _        _       ");
        System.out.println(" |  __ \\               / ____| |      | |      ");
        System.out.println(" | |__) |   _ _ __ ___| (___ | |_ __ _| |_ ___ ");
        System.out.println(" |  ___/ | | | '__/ _ \\\\___ \\| __/ _` | __/ __|");
        System.out.println(" | |   | |_| | | |  __/____) | || (_| | |_\\__ \\");
        System.out.println(" |_|    \\__,_|_|  \\___|_____/ \\__\\__,_|\\__|___/");

        if (!getDataFolder().exists()){
            boolean done = getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");


        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Configuration config = null;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            System.out.println("[PureStats] Couldn't load default config: "+e.getMessage());
        }

        assert config != null;
        String direction = config.getString("mysql.direction");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        boolean ssl = config.getBoolean("mysql.SSL");


        try {
            sqlconn = "jdbc:mysql://" + direction + ":" + port + "/" + database + "?user=" + username + "&password=" + password + "&useSSL=" + ssl;
            Connection conn = DriverManager.getConnection(sqlconn);
            System.out.println("[PureStats] Connected to the database");
            conn.close();
        } catch (SQLException e) {
            System.out.println("[PureStats] Couldn't connect to the database");
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }

        if(!helloTable()){
            System.out.println("[PureStats] User table doesn't exists");
            if(setupDatabase(database)){
                System.out.println("[PureStats] Created user table");
            }
        }

        if(helloTable()){
            getProxy().getPluginManager().registerListener(this, new join());
        } else {
            System.out.println("[PureStats] User table was not created, please, check your config and restart your server");
        }

    }

    public boolean helloTable(){

        DatabaseMetaData dbm;
        {
            try {
                Connection conn = DriverManager.getConnection(sqlconn);

                dbm = conn.getMetaData();
                ResultSet tables;
                tables = dbm.getTables(null, null, "pure_stats", null);

                conn.close();
                if (tables.next()) {
                    return true;
                }
                else {
                    return false;
                }

            } catch (SQLException e) {
                System.out.println("[PureStats] Couldn't check the push table");
                System.out.println("SQLException: " + e.getMessage());
                System.out.println("SQLState: " + e.getSQLState());
                System.out.println("VendorError: " + e.getErrorCode());
                return false;
            }
        }
    }

    public boolean setupDatabase(String dbname){
        try {
            Connection conn = DriverManager.getConnection(sqlconn);

            Statement stmt = conn.createStatement();
            int rs = stmt.executeUpdate("CREATE TABLE `"+dbname+"`.`pure_stats` ( `uuid` VARCHAR(36) NOT NULL , `name` VARCHAR(16) NOT NULL , `lastip` VARCHAR(30) NULL , `country` VARCHAR(2) NULL DEFAULT NULL , `state` VARCHAR(2) NULL DEFAULT NULL , `playtime` BIGINT NOT NULL DEFAULT '0' , `registered` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, `id` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`) ) ENGINE = InnoDB;");

            conn.close();
            return true;
        } catch (SQLException e) {
            System.out.println("[PureStats] Couldn't create the table");
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
            return false;
        }

    }
}
