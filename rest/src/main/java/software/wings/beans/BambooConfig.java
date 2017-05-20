package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

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

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
public class BambooConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Bamboo URL", required = true) @NotEmpty private String bambooUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new BambooService config.
   */
  public BambooConfig() {
    super(SettingVariableTypes.BAMBOO.name());
  }

  /**
   * Gets bamboos url.
   *
   * @return the bamboos url
   */
  public String getBambooUrl() {
    return bambooUrl;
  }

  /**
   * Sets bamboos url.
   *
   * @param bambooUrl the bamboos url
   */
  public void setBambooUrl(String bambooUrl) {
    this.bambooUrl = bambooUrl;
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

    BambooConfig that = (BambooConfig) o;

    if (!bambooUrl.equals(that.bambooUrl))
      return false;
    if (!username.equals(that.username))
      return false;
    if (!Arrays.equals(password, that.password))
      return false;
    return accountId.equals(that.accountId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bambooUrl, username, password, accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("bambooUrl", bambooUrl)
        .add("username", username)
        .add("accountId", accountId)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private char[] password;
    private String username;
    private String bambooUrl;
    private String accountId;

    private Builder() {}

    /**
     * A bamboo config builder.
     *
     * @return the builder
     */
    public static Builder aBambooConfig() {
      return new Builder();
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
     * With bamboo url builder.
     *
     * @param bambooUrl the bamboo url
     * @return the builder
     */
    public Builder withBambooUrl(String bambooUrl) {
      this.bambooUrl = bambooUrl;
      return this;
    }

    /**
     * With accountId.
     *
     * @param accountId the accountId
     * @return the builder
     */
    public BambooConfig.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBambooConfig().withPassword(password).withUsername(username).withBambooUrl(bambooUrl).withAccountId(
          accountId);
    }

    /**
     * Build bamboo config.
     *
     * @return the bamboo config
     */
    public BambooConfig build() {
      BambooConfig bambooConfig = new BambooConfig();
      bambooConfig.setPassword(password);
      bambooConfig.setUsername(username);
      bambooConfig.setBambooUrl(bambooUrl);
      bambooConfig.setAccountId(accountId);
      return bambooConfig;
    }
  }
}
