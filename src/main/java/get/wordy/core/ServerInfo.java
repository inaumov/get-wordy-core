package get.wordy.core;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ServerInfo {

    private String host;
    private String database;
    private Properties account;
    private Set<String> parameters = new HashSet<>();

    public ServerInfo() {
        account = new Properties();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Properties getAccount() {
        return account;
    }

    public void setAccount(Properties account) {
        this.account.clear();
        this.account.putAll(account);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    public void addParameter(String parameter) {
        parameters.add(parameter);
    }

}