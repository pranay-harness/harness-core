package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import ro.fortsoft.pf4j.Extension;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;

import java.util.Objects;

/**
 * Created by anubhaw on 8/4/16.
 */
@Extension
@JsonTypeName("APP_DYNAMICS")
public class AppDynamicsConfig extends SettingValue {
  @Attributes(title = "User Name") private String username;
  @Attributes(title = "Account Name") private String accountname;
  @JsonView(JsonViews.Internal.class) @Attributes(title = "Password") private String password;
  @Attributes(title = "Controller URL") private String controllerUrl;

  /**
   * Instantiates a new App dynamics config.
   */
  public AppDynamicsConfig() {
    super(StateType.APP_DYNAMICS.name());
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
   * Gets accountname.
   *
   * @return the accountname
   */
  public String getAccountname() {
    return accountname;
  }

  /**
   * Sets accountname.
   *
   * @param accountname the accountname
   */
  public void setAccountname(String accountname) {
    this.accountname = accountname;
  }

  /**
   * Gets password.
   *
   * @return the password
   */
  //@JsonIgnore
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  //@JsonProperty
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets controller url.
   *
   * @return the controller url
   */
  public String getControllerUrl() {
    return controllerUrl;
  }

  /**
   * Sets controller url.
   *
   * @param controllerUrl the controller url
   */
  public void setControllerUrl(String controllerUrl) {
    this.controllerUrl = controllerUrl;
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, accountname, password, controllerUrl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final AppDynamicsConfig other = (AppDynamicsConfig) obj;
    return Objects.equals(this.username, other.username) && Objects.equals(this.accountname, other.accountname)
        && Objects.equals(this.password, other.password) && Objects.equals(this.controllerUrl, other.controllerUrl);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("username", username)
        .add("accountname", accountname)
        .add("password", password)
        .add("controllerUrl", controllerUrl)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String username;
    private String accountname;
    private String password;
    private String controllerUrl;

    private Builder() {}

    /**
     * An app dynamics config builder.
     *
     * @return the builder
     */
    public static Builder anAppDynamicsConfig() {
      return new Builder();
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
     * With accountname builder.
     *
     * @param accountname the accountname
     * @return the builder
     */
    public Builder withAccountname(String accountname) {
      this.accountname = accountname;
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
     * With controller url builder.
     *
     * @param controllerUrl the controller url
     * @return the builder
     */
    public Builder withControllerUrl(String controllerUrl) {
      this.controllerUrl = controllerUrl;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAppDynamicsConfig()
          .withUsername(username)
          .withAccountname(accountname)
          .withPassword(password)
          .withControllerUrl(controllerUrl);
    }

    /**
     * Build app dynamics config.
     *
     * @return the app dynamics config
     */
    public AppDynamicsConfig build() {
      AppDynamicsConfig appDynamicsConfig = new AppDynamicsConfig();
      appDynamicsConfig.setUsername(username);
      appDynamicsConfig.setAccountname(accountname);
      appDynamicsConfig.setPassword(password);
      appDynamicsConfig.setControllerUrl(controllerUrl);
      return appDynamicsConfig;
    }
  }
}
