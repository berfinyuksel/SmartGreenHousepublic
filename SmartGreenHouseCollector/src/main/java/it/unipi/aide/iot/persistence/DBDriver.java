package it.unipi.aide.iot.persistence;

import it.unipi.aide.iot.bean.HumiditySample;
import it.unipi.aide.iot.bean.LightIntensitySample;
import it.unipi.aide.iot.bean.TemperatureSample;
import it.unipi.aide.iot.config.ConfigurationParameters;

import java.sql.*;

public class DBDriver {
    private static DBDriver instance = null;

    private static String databaseIp;
    private static int databasePort;
    private static String databaseUsername;
    private static String databasePassword;
    private static String databaseName;

    public static DBDriver getInstance() {
        if(instance == null)
            instance = new DBDriver();

        return instance;
    }

    private DBDriver() {
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        databaseIp = configurationParameters.getDatabaseIp();
        databasePort = configurationParameters.getDatabasePort();
        databaseUsername = configurationParameters.getDatabaseUsername();
        databasePassword = configurationParameters.getDatabasePassword();
        databaseName = configurationParameters.getDatabaseName();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp + ":" + databasePort +
                        "/" + databaseName + "?useSSL=NO&zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }

    public void insertActuator(String ip, String type) {
        try (
                PreparedStatement statement = getConnection().prepareStatement("INSERT INTO actuators (ip, name, timestamp, state) VALUES (?, ?, ?, ?)")
        )
        {
            statement.setString(1, ip);
            statement.setString(2, type);
            statement.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            statement.setString(4, "ON");
            statement.executeUpdate();

            System.out.println("[REGISTRATION] The "+type+" device [" + ip + "] is now registered\n");
        }
        catch (final SQLIntegrityConstraintViolationException e)
        {
            System.out.printf("INFO: band device %s already registered in the database.%n", ip);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeActuator(String ip, String type){
        try (
                //Connection connection = getConnection();
                PreparedStatement statement = getConnection().prepareStatement("DELETE FROM actuators WHERE ip = ? and type = ?")
        )
        {
            statement.setString(1, ip);
            statement.setString(2, type);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void insertTemperatureSample(TemperatureSample temperatureSample) {
        try (
                //Connection connection = getConnection();
                PreparedStatement statement = getConnection().prepareStatement("INSERT INTO temperature (nodeId, degrees, timestamp) VALUES (?, ?, ?)")
        )
        {
            statement.setInt(1, temperatureSample.getNodeId());
            statement.setInt(2, temperatureSample.getTemperature());
            statement.setTimestamp(3, temperatureSample.getTimestamp());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void insertLightIntensitySample(LightIntensitySample lightintensitysample) {
        try (
                PreparedStatement statement = getConnection().prepareStatement("INSERT INTO light (nodeId, level, timestamp) VALUES (?, ?, ?)")
        )
        {
            statement.setInt(1, lightintensitysample.getNode());
            statement.setInt(2, lightintensitysample.getIntensity());
            statement.setTimestamp(3, lightintensitysample.getTimestamp());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

       public void insertHumiditySample(HumiditySample humiditysample) {
        try (
                PreparedStatement statement = getConnection().prepareStatement("INSERT INTO humidity (nodeId, level, timestamp) VALUES (?, ?, ?)")
        )
        {
            statement.setInt(1, humiditysample.getNode());
            statement.setInt(2, humiditysample.getHumidity());
            statement.setTimestamp(3, humiditysample.getTimestamp());
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

}
