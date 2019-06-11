package ovh.quiquelhappy.mcplugins.purestats.listener.listener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ovh.quiquelhappy.mcplugins.purestats.listener.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class join implements Listener {

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        ProxiedPlayer player = event.getPlayer();

        getLocation(ip, player);

    }

    private static void getLocation(String ip, ProxiedPlayer player){
        URL url;
        try {
            url = new URL("https://geoip-db.com/json/"+ip);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            con.disconnect();

            String response = content.toString();
            JsonObject obj = new JsonParser().parse(response).getAsJsonObject();

            String country = null;
            String state = null;

            if(!obj.get("country_code").isJsonNull()){
                if(!obj.get("country_code").getAsString().equals("Not found")){
                    country=obj.get("country_code").getAsString();
                }
            }

            if(!obj.get("state").isJsonNull()){
                if(!obj.get("state").getAsString().equals("Not found")){
                    state=obj.get("state").getAsString().replace("'","");
                }
            }

            Connection conn = DriverManager.getConnection(main.sqlconn);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT uuid FROM pure_stats WHERE uuid = '"+player.getUniqueId().toString()+"'");

            if(rs.next()){
                updatePlayer(player,country,state,ip);
            } else {
                registerPlayer(player,country,state,ip);
            }

            conn.close();

        } catch (IOException | SQLException e) {
            System.out.println("[PureStats] Error while getting "+player.getName()+" location");
            e.printStackTrace();
        }

    }

    public static void updatePlayer(ProxiedPlayer player, String country, String state, String ip){

        String fcountry;
        String fstate;

        if(country!=null){
            if(!country.equals("Not found")){
                fcountry=country;
            } else {
                fcountry=null;
            }
        } else {
            fcountry=null;
        }

        if(state!=null){
            if(!state.equals("Not found")){
                fstate=state;
            } else {
                fstate=null;
            }
        } else {
            fstate=null;
        }

        Statement stmt;
        try {

            Connection conn = DriverManager.getConnection(main.sqlconn);
            stmt = conn.createStatement();

            if(fcountry==null){
                System.out.println("[PureStats] Updating "+player.getName()+" with the uuid "+player.getUniqueId().toString());
                stmt.executeUpdate("UPDATE `pure_stats` SET `name`='"+player.getName()+"',`lastip`='"+ip+"' WHERE uuid='"+player.getUniqueId().toString()+"'");
            } else {
                if(fstate==null){
                    System.out.println("[PureStats] Updating "+player.getName()+" from "+fcountry+" with the uuid "+player.getUniqueId().toString());
                    stmt.executeUpdate("UPDATE `pure_stats` SET `name`='"+player.getName()+"',`lastip`='"+ip+"',`country`='"+fcountry+"' WHERE uuid='"+player.getUniqueId().toString()+"'");
                } else {
                    System.out.println("[PureStats] Updating "+player.getName()+" from "+fstate+", "+fcountry+" with the uuid "+player.getUniqueId().toString());
                    stmt.executeUpdate("UPDATE `pure_stats` SET `name`='"+player.getName()+"',`lastip`='"+ip+"',`country`='"+fcountry+"',`state`='"+fstate+"' WHERE uuid='"+player.getUniqueId().toString()+"'");
                }
            }

            conn.close();

        } catch (SQLException e) {
            System.out.println("[PureStats] Couldn't update player");
        }
    }

    public static void registerPlayer(ProxiedPlayer player, String country, String state, String ip){
        String fcountry;
        String fstate;

        if(country!=null){
            if(!country.equals("Not found")){
                fcountry=country;
            } else {
                fcountry=null;
            }
        } else {
            fcountry=null;
        }

        if(state!=null){
            if(!state.equals("Not found")){
                fstate=state;
            } else {
                fstate=null;
            }
        } else {
            fstate=null;
        }

        Statement stmt;
        try {

            Connection conn = DriverManager.getConnection(main.sqlconn);
            stmt = conn.createStatement();

            if(fcountry==null){
                System.out.println("[PureStats] Registering "+player.getName());
                int affected = stmt.executeUpdate("INSERT INTO `pure_stats`(`uuid`, `name`, `lastip`) VALUES ('"+player.getUniqueId().toString()+"','"+player.getName()+"','"+ip+"')");
                if(affected==0){
                    System.out.println("[PureStats] Error while registering"+player.getName());
                }
            } else {
                if(fstate==null){
                    System.out.println("[PureStats] Registering "+player.getName()+" from "+fcountry);
                    int affected = stmt.executeUpdate("INSERT INTO `pure_stats`(`uuid`, `name`, `lastip`, `country`) VALUES ('"+player.getUniqueId().toString()+"','"+player.getName()+"','"+ip+"','"+fcountry+"')");
                    if(affected==0){
                        System.out.println("[PureStats] Error while registering"+player.getName());
                    }

                } else {
                    System.out.println("[PureStats] Registering "+player.getName()+" from "+fstate+", "+fcountry);
                    int affected = stmt.executeUpdate("INSERT INTO `pure_stats`(`uuid`, `name`, `lastip`, `country`, `state`) VALUES ('"+player.getUniqueId().toString()+"','"+player.getName()+"','"+ip+"','"+fcountry+"','"+fstate+"')");
                    if(affected==0){
                        System.out.println("[PureStats] Error while registering"+player.getName());
                    }
                }
            }

            conn.close();

        } catch (SQLException e) {
            System.out.println("[PureStats] Couldn't register player");
        }
    }

}