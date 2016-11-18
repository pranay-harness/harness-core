package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * The type Splunk config.
 */
@JsonTypeName("SPLUNK")
public class SplunkConfig extends SettingValue {
  @Attributes(title = "Host") private String host;
  @Attributes(title = "Port") private int port;
  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password") private String password;

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  /**
   * Gets host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets port.
   *
   * @param port the port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets username.
   *
   * @param username the username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Gets password.
   *
   * @return the password
   */
  @JsonIgnore
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  @JsonProperty
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String host;
    private int port;
    private String username;
    private String password;

    private Builder() {}

    /**
     * A splunk config builder.
     *
     * @return the builder
     */
    public static Builder aSplunkConfig() {
      return new Builder();
    }

    /**
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    /**
     * With port builder.
     *
     * @param port the port
     * @return the builder
     */
    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    /**
     * With username builder.
     *
     * @param username the username
     * @return the builder
     */
    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * With password builder.
     *
     * @param password the password
     * @return the builder
     */
    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSplunkConfig().withHost(host).withPort(port).withUsername(username).withPassword(password);
    }

    /**
     * Build splunk config.
     *
     * @return the splunk config
     */
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
