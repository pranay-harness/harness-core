package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

/**
 * Created by peeyushaggarwal on 5/26/16.
 */
public class JenkinsConfig extends SettingValue {
  @URL private String jenkinsUrl;
  @NotEmpty private String username;
  @NotEmpty private String password;

  public JenkinsConfig() {
    super(SettingVariableTypes.JENKINS);
  }

  public String getJenkinsUrl() {
    return jenkinsUrl;
  }

  public void setJenkinsUrl(String jenkinsUrl) {
    this.jenkinsUrl = jenkinsUrl;
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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    JenkinsConfig that = (JenkinsConfig) o;
    return Objects.equal(jenkinsUrl, that.jenkinsUrl) && Objects.equal(username, that.username)
        && Objects.equal(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(jenkinsUrl, username, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("jenkinsUrl", jenkinsUrl)
        .add("username", username)
        .add("password", password)
        .toString();
  }

  public static final class Builder {
    private String jenkinsUrl;
    private String username;
    private String password;

    private Builder() {}

    public static Builder aJenkinsConfig() {
      return new Builder();
    }

    public Builder withJenkinsUrl(String jenkinsUrl) {
      this.jenkinsUrl = jenkinsUrl;
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
      return aJenkinsConfig().withJenkinsUrl(jenkinsUrl).withUsername(username).withPassword(password);
    }

    public JenkinsConfig build() {
      JenkinsConfig jenkinsConfig = new JenkinsConfig();
      jenkinsConfig.setJenkinsUrl(jenkinsUrl);
      jenkinsConfig.setUsername(username);
      jenkinsConfig.setPassword(password);
      return jenkinsConfig;
    }
  }
}
