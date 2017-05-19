package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.List;

/**
 * The type Splunk config.
 */
@JsonTypeName("SPLUNK")
public class SplunkConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Host", required = true) @NotEmpty private String host;
  @Attributes(title = "Port", required = true) private int port;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;
  @Attributes(title = "Encrypted Fields", required = true)
  private List<String> encryptedFields = Arrays.asList("password");

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  /**
   * Gets the list of fields that are encrypted for use in the UI
   * @return List of field names
   */
  public List<String> getEncryptedFields() {
    return encryptedFields;
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
  //@JsonIgnore
  public char[] getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  //@JsonProperty
  public void setPassword(char[] password) {
    this.password = password;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    SplunkConfig that = (SplunkConfig) o;

    if (port != that.port)
      return false;
    if (!host.equals(that.host))
      return false;
    if (!username.equals(that.username))
      return false;
    if (!Arrays.equals(password, that.password))
      return false;
    return accountId.equals(that.accountId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(port, username, password, accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("host", host)
        .add("port", port)
        .add("username", username)
        .add("accountId", accountId)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String host;
    private int port;
    private String username;
    private char[] password;
    private String accountId;

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
    public Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    /**
     * With accountId.
     *
     * @param accountId the accountId
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSplunkConfig().withHost(host).withPort(port).withUsername(username).withPassword(password).withAccountId(
          accountId);
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
      splunkConfig.setAccountId(accountId);
      return splunkConfig;
    }
  }
}
