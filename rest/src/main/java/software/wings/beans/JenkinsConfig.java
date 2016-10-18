package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import software.wings.settings.SettingValue;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
@JsonTypeName("JENKINS")
public class JenkinsConfig extends SettingValue {
  @URL private String jenkinsUrl;
  @NotEmpty private String username;
  @NotEmpty private String password;

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
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
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
        && Objects.equal(password, that.password);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(jenkinsUrl, username, password);
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
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String jenkinsUrl;
    private String username;
    private String password;

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
      return aJenkinsConfig().withJenkinsUrl(jenkinsUrl).withUsername(username).withPassword(password);
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
      return jenkinsConfig;
    }
  }
}
