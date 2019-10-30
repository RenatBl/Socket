package ru.itis.socket.services;

import com.beust.jcommander.Parameter;

public class Args {
    @Parameter(names  = {"--port","--p"}, description = "port to char server")
    private String port;
    @Parameter(names  = {"--db-properties","--prop"}, description = "location of properties file")
    private String path_prop;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPath_prop() {
        return path_prop;
    }

    public void setPath_prop(String path_prop) {
        this.path_prop = path_prop;
    }
}
