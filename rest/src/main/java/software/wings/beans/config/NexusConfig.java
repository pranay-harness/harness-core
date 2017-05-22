package software.wings.beans.config;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by srinivas on 3/30/17.
 */
@JsonTypeName("NEXUS")
public class NexusConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Nexus URL", required = true) @NotEmpty private String nexusUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @Encrypted
  @NotEmpty
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new Nexus config.
   */
  public NexusConfig() {
    super(SettingVariableTypes.NEXUS.name());
  }

  public String getNexusUrl() {
    return nexusUrl;
  }

  public void setNexusUrl(String nexusUrl) {
    this.nexusUrl = nexusUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NexusConfig other = (NexusConfig) o;
    return Objects.equals(this.nexusUrl, other.nexusUrl) && Arrays.equals(this.password, other.password)
        && Objects.equals(this.username, other.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nexusUrl, username, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("nexusUrl", nexusUrl).add("username", username).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private char[] password;
    private String username;
    private String nexusUrl;
    private String accountId;

    private Builder() {}

    /**
     * A nexus config builder.
     *
     * @return the builder
     */
    public static NexusConfig.Builder aNexusConfig() {
      return new NexusConfig.Builder();
    }

    /**
     * With password builder.
     *
     * @param password the password
     * @return the builder
     */
    public NexusConfig.Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    /**
     * With username builder.
     *
     * @param username the username
     * @return the builder
     */
    public NexusConfig.Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * With nexus url builder.
     *
     * @param nexusUrl the nexuss url
     * @return the builder
     */
    public NexusConfig.Builder withNexusUrl(String nexusUrl) {
      this.nexusUrl = nexusUrl;
      return this;
    }

    /**
     * With accountId.
     *
     * @param accountId the accountId
     * @return the builder
     */
    public NexusConfig.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public NexusConfig.Builder but() {
      return aNexusConfig().withPassword(password).withUsername(username).withNexusUrl(nexusUrl).withAccountId(
          accountId);
    }

    /**
     * Build nexus config.
     *
     * @return the nexus config
     */
    public NexusConfig build() {
      NexusConfig nexusConfig = new NexusConfig();
      nexusConfig.setPassword(password);
      nexusConfig.setUsername(username);
      nexusConfig.setNexusUrl(nexusUrl);
      nexusConfig.setAccountId(accountId);
      return nexusConfig;
    }
  }
}
