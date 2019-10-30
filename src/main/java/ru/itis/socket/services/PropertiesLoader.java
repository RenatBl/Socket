package ru.itis.socket.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesLoader {
    public static String[] getProperties(String path) {
        Properties properties = new Properties();
        String dbUrl;
        String dbUsername;
        String dbPassword;
        String driverClassName;
        try {
            properties.load(new FileInputStream(new File(path)));
            dbUrl = properties.getProperty("db.url");
            dbUsername = properties.getProperty("db.username");
            dbPassword = properties.getProperty("db.password");
            driverClassName = properties.getProperty("db.driverClassName");
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
        return (new String[]{dbUrl, dbUsername, dbPassword, driverClassName});
    }
}