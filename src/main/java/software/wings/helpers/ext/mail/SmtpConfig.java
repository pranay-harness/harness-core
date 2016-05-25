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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SmtpConfig that = (SmtpConfig) obj;
    return port == that.port && useSSL == that.useSSL && Objects.equal(host, that.host)
        && Objects.equal(username, that.username) && Objects.equal(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(host, port, useSSL, username, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("host", host)
        .add("port", port)
        .add("useSSL", useSSL)
        .add("username", username)
        .add("password", password)
        .toString();
  }

  public static final class Builder {
    private String host;
    private int port;
    private boolean useSSL;
    private String username;
    private String password;

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

    public Builder but() {
      return aSmtpConfig().withHost(host).withPort(port).withUseSSL(useSSL).withUsername(username).withPassword(
          password);
    }

    public SmtpConfig build() {
      SmtpConfig smtpConfig = new SmtpConfig();
      smtpConfig.setHost(host);
      smtpConfig.setPort(port);
      smtpConfig.setUseSSL(useSSL);
      smtpConfig.setUsername(username);
      smtpConfig.setPassword(password);
      return smtpConfig;
    }
  }
}
