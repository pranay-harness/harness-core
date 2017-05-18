package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 8/4/16.
 */
@Extension
@JsonTypeName("APP_DYNAMICS")
public class AppDynamicsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Account Name", required = true) @NotEmpty private String accountname;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @Attributes(title = "Controller URL", required = true) @NotEmpty private String controllerUrl;
  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new App dynamics config.
   */
  public AppDynamicsConfig() {
    super(StateType.APP_DYNAMICS.name());
  }

  /**
   * Gets list of field names which are encrypted so the UI can handle them properly.
   * @return List of field names
   */
  @Override
  public List<String> getEncryptedFieldNames() {
    return new ArrayList<>(Arrays.asList("password"));
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
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
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
        && Arrays.equals(this.password, other.password) && Objects.equals(this.controllerUrl, other.controllerUrl);
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
    private char[] password;
    private String controllerUrl;
    private String accountId;

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
    public Builder withPassword(char[] password) {
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
      return anAppDynamicsConfig()
          .withUsername(username)
          .withAccountname(accountname)
          .withPassword(password)
          .withControllerUrl(controllerUrl)
          .withAccountId(accountId);
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
      appDynamicsConfig.setAccountId(accountId);
      return appDynamicsConfig;
    }
  }
}
