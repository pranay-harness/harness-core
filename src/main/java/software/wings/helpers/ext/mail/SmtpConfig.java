package software.wings.helpers.ext.mail;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import software.wings.beans.SettingValue;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class SmtpConfig extends SettingValue {
  private String host;
  private int port;
  private String fromAddress;
  private boolean useSSL;
  private String username;
  private String password;

  public SmtpConfig() {
    super(SettingVariableTypes.SMTP);
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

  public boolean isUseSSL() {
    return useSSL;
  }

  public void setUseSSL(boolean useSSL) {
    this.useSSL = useSSL;
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

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SmtpConfig config = (SmtpConfig) o;
    return port == config.port && useSSL == config.useSSL && Objects.equal(host, config.host)
        && Objects.equal(fromAddress, config.fromAddress) && Objects.equal(username, config.username)
        && Objects.equal(password, config.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(host, port, fromAddress, useSSL, username, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("host", host)
        .add("port", port)
        .add("fromAddress", fromAddress)
        .add("useSSL", useSSL)
        .add("username", username)
        .add("password", password)
        .toString();
  }

  public static final class Builder {
    private String host;
    private int port;
    private String fromAddress;
    private boolean useSSL;
    private String username;
    private String password;
    private SettingVariableTypes type;

    private Builder() {}

    public static Builder aSmtpConfig() {
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

    public Builder withFromAddress(String fromAddress) {
      this.fromAddress = fromAddress;
      return this;
    }

    public Builder withUseSSL(boolean useSSL) {
      this.useSSL = useSSL;
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

    public Builder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    public Builder but() {
      return aSmtpConfig()
          .withHost(host)
          .withPort(port)
          .withFromAddress(fromAddress)
          .withUseSSL(useSSL)
          .withUsername(username)
          .withPassword(password)
          .withType(type);
    }

    public SmtpConfig build() {
      SmtpConfig smtpConfig = new SmtpConfig();
      smtpConfig.setHost(host);
      smtpConfig.setPort(port);
      smtpConfig.setFromAddress(fromAddress);
      smtpConfig.setUseSSL(useSSL);
      smtpConfig.setUsername(username);
      smtpConfig.setPassword(password);
      smtpConfig.setType(type);
      return smtpConfig;
    }
  }
}
