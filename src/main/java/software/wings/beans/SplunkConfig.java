package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SPLUNK")
public class SplunkConfig extends SettingValue {
  private String host;
  private int port;
  private String username;
  private String password;

  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK);
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static final class Builder {
    private String host;
    private int port;
    private String username;
    private String password;

    private Builder() {}

    public static Builder aSplunkConfig() {
      return new Builder();
    }

    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder but() {
      return aSplunkConfig().withHost(host).withPort(port).withUsername(username).withPassword(password);
    }

    public SplunkConfig build() {
      SplunkConfig splunkConfig = new SplunkConfig();
      splunkConfig.setHost(host);
      splunkConfig.setPort(port);
      splunkConfig.setUsername(username);
      splunkConfig.setPassword(password);
      return splunkConfig;
    }
  }
}
