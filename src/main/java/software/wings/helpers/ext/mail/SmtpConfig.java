package software.wings.helpers.ext.mail;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import software.wings.beans.SettingValue;

// TODO: Auto-generated Javadoc

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

  /**
   * Instantiates a new smtp config.
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(host, port, fromAddress, useSSL, username, password);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String host;
    private int port;
    private String fromAddress;
    private boolean useSSL;
    private String username;
    private String password;

    private Builder() {}

    /**
     * A smtp config.
     *
     * @return the builder
     */
    public static Builder aSmtpConfig() {
      return new Builder();
    }

    /**
     * With host.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    /**
     * With port.
     *
     * @param port the port
     * @return the builder
     */
    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * With from address.
     *
     * @param fromAddress the from address
     * @return the builder
     */
    public Builder withFromAddress(String fromAddress) {
      this.fromAddress = fromAddress;
      return this;
    }

    /**
     * With use ssl.
     *
     * @param useSSL the use ssl
     * @return the builder
     */
    public Builder withUseSSL(boolean useSSL) {
      this.useSSL = useSSL;
      return this;
    }

    /**
     * With username.
     *
     * @param username the username
     * @return the builder
     */
    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * With password.
     *
     * @param password the password
     * @return the builder
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aSmtpConfig()
          .withHost(host)
          .withPort(port)
          .withFromAddress(fromAddress)
          .withUseSSL(useSSL)
          .withUsername(username)
          .withPassword(password);
    }

    /**
     * Builds the.
     *
     * @return the smtp config
     */
    public SmtpConfig build() {
      SmtpConfig smtpConfig = new SmtpConfig();
      smtpConfig.setHost(host);
      smtpConfig.setPort(port);
      smtpConfig.setFromAddress(fromAddress);
      smtpConfig.setUseSSL(useSSL);
      smtpConfig.setUsername(username);
      smtpConfig.setPassword(password);
      return smtpConfig;
    }
  }
}
