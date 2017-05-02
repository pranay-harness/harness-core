package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

import java.util.Arrays;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
@JsonTypeName("JENKINS")
public class JenkinsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Jenkins URL", required = true) @URL @NotEmpty private String jenkinsUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  public char[] password;
  @Attributes(title = "Account ID") @NotEmpty private String accountId;
  /**
   * Instantiates a new jenkins config.
   */
  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS.name());
  }

  /**
   * Gets jenkins url.
   *
   * @return the jenkins url
   */
  public String getJenkinsUrl() {
    return jenkinsUrl;
  }

  /**
   * Sets jenkins url.
   *
   * @param jenkinsUrl the jenkins url
   */
  public void setJenkinsUrl(String jenkinsUrl) {
    this.jenkinsUrl = jenkinsUrl;
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
  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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
    JenkinsConfig that = (JenkinsConfig) o;
    return Objects.equal(jenkinsUrl, that.jenkinsUrl) && Objects.equal(username, that.username)
        && Arrays.equals(password, that.password) && Objects.equal(accountId, that.accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(jenkinsUrl, username, password, accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jenkinsUrl", jenkinsUrl)
        .add("username", username)
        .add("password", password)
        .add("accountId", accountId)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String jenkinsUrl;
    private String username;
    private char[] password;
    private String accountId;

    private Builder() {}

    /**
     * A jenkins config.
     *
     * @return the builder
     */
    public static Builder aJenkinsConfig() {
      return new Builder();
    }

    /**
     * With jenkins url.
     *
     * @param jenkinsUrl the jenkins url
     * @return the builder
     */
    public Builder withJenkinsUrl(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
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
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aJenkinsConfig()
          .withJenkinsUrl(jenkinsUrl)
          .withUsername(username)
          .withPassword(password)
          .withAccountId(accountId);
    }

    /**
     * Builds the.
     *
     * @return the jenkins config
     */
    public JenkinsConfig build() {
      JenkinsConfig jenkinsConfig = new JenkinsConfig();
      jenkinsConfig.setJenkinsUrl(jenkinsUrl);
      jenkinsConfig.setUsername(username);
      jenkinsConfig.setPassword(password);
      jenkinsConfig.setAccountId(accountId);
      return jenkinsConfig;
    }
  }
}
