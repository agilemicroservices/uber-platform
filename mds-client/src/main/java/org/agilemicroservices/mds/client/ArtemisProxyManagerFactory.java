package org.agilemicroservices.mds.client;

import javax.jms.ConnectionFactory;


public class ArtemisProxyManagerFactory implements ProxyManagerFactory {
    private String url;
    private String username;
    private String password;


    public static ConnectionFactory createProxyManager(String url, String user, String password) {
        return null;
    }


    @Override
    public ProxyManager createProxyManager() {
        return null;
    }


    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
