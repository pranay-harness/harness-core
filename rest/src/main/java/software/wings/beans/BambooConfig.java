package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import software.wings.settings.SettingValue;

import java.util.Objects;

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
public class BambooConfig extends SettingValue {
  @Attributes(title = "Bamboo URL") @URL private String bambooUrl;
  @Attributes(title = "Username") @NotEmpty private String username;
  @Attributes(title = "Password") @NotEmpty private String password;

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

  @Override
  public int hashCode() {
    return Objects.hash(bambooUrl, username, password);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BambooConfig other = (BambooConfig) obj;
    return Objects.equals(this.bambooUrl, other.bambooUrl) && Objects.equals(this.username, other.username)
        && Objects.equals(this.password, other.password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("bambooUrl", bambooUrl)
        .add("username", username)
        .add("password", password)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String password;
    private String username;
    private String bamboosUrl;

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
    public Builder withPassword(String password) {
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
     * With bamboos url builder.
     *
     * @param bamboosUrl the bamboos url
     * @return the builder
     */
    public Builder withBamboosUrl(String bamboosUrl) {
      this.bamboosUrl = bamboosUrl;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBambooConfig().withPassword(password).withUsername(username).withBamboosUrl(bamboosUrl);
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
      bambooConfig.setBambooUrl(bamboosUrl);
      return bambooConfig;
    }
  }
}
