package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/22/16.
 */
@JsonTypeName("KUBERNETES")
public class KubernetesConfig extends SettingValue {
  @Attributes(title = "API server url") private String apiServerUrl;
  @Attributes(title = "User Name") private String username;
  @Attributes(title = "Password") private String password;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  /**
   * Gets api server url.
   *
   * @return the api server url
   */
  public String getApiServerUrl() {
    return apiServerUrl;
  }

  /**
   * Sets api server url.
   *
   * @param apiServerUrl the api server url
   */
  public void setApiServerUrl(String apiServerUrl) {
    this.apiServerUrl = apiServerUrl;
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
    private String apiServerUrl;
    private String username;
    private String password;

    private Builder() {}

    /**
     * A kubernetes config builder.
     *
     * @return the builder
     */
    public static Builder aKubernetesConfig() {
      return new Builder();
    }

    /**
     * With api server url builder.
     *
     * @param apiServerUrl the api server url
     * @return the builder
     */
    public Builder withApiServerUrl(String apiServerUrl) {
      this.apiServerUrl = apiServerUrl;
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
      return aKubernetesConfig().withApiServerUrl(apiServerUrl).withUsername(username).withPassword(password);
    }

    /**
     * Build kubernetes config.
     *
     * @return the kubernetes config
     */
    public KubernetesConfig build() {
      KubernetesConfig kubernetesConfig = new KubernetesConfig();
      kubernetesConfig.setApiServerUrl(apiServerUrl);
      kubernetesConfig.setUsername(username);
      kubernetesConfig.setPassword(password);
      return kubernetesConfig;
    }
  }
}
