package it.unipi.aide.iot.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigurationParameters {
    private static volatile ConfigurationParameters instance;

    private String databaseIp;
    private int databasePort;
    private String databaseUsername;
    private String databasePassword;
    private String databaseName;


    public static ConfigurationParameters getInstance() {
        if (instance == null) {
            synchronized (ConfigurationParameters.class) {
                if (instance == null) {
                    instance = readConfigurationParameters();
                }
            }
        }
        return instance;
    }

    private static ConfigurationParameters readConfigurationParameters() {
        if (validConfigurationParameters()) {
            XStream xs = new XStream();
            xs.addPermission(AnyTypePermission.ANY);

            String text = null;
            try {
                text = new String(Files.readAllBytes(Paths.get("config.xml")));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            return (ConfigurationParameters) xs.fromXML(text);
        } else {
            System.err.println("Problems with the configuration file!");
            System.exit(1);
        }
        return null;
    }

    private static boolean validConfigurationParameters() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Document document = documentBuilder.parse("config.xml");
            Schema schema = schemaFactory.newSchema(new StreamSource("./config.xsd"));
            schema.newValidator().validate(new DOMSource(document));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    public String getDatabaseIp() {
        return databaseIp;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }

}
