package get.wordy.core.db;

import java.util.Properties;

public class ServerInfo {

    private String url;
    private Properties credentials;

    public ServerInfo() {
        credentials = new Properties();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String host) {
        this.url = host;
    }

    public Properties getCredentials() {
        return credentials;
    }

    public void setCredentials(Properties credentials) {
        this.credentials.clear();
        this.credentials.putAll(credentials);
    }

}