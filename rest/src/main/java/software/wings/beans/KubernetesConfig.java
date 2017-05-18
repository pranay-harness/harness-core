package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("KUBERNETES")
public class KubernetesConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Cluster master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @NotEmpty @Encrypted private char[] password;
  @NotEmpty @SchemaIgnore private String accountId;

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
  public String getMasterUrl() {
    return masterUrl;
  }

  /**
   * Sets api server url.
   *
   * @param masterUrl the api server url
   */
  public void setMasterUrl(String masterUrl) {
    this.masterUrl = masterUrl;
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
  public char[] getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  @JsonProperty
  public void setPassword(char[] password) {
    this.password = password;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String masterUrl;
    private String username;
    private char[] password;
    private String accountId;

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
     * @param masterUrl the api server url
     * @return the builder
     */
    public Builder withMasterUrl(String masterUrl) {
      this.masterUrl = masterUrl;
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
      return aKubernetesConfig().withMasterUrl(masterUrl).withUsername(username).withPassword(password).withAccountId(
          accountId);
    }

    /**
     * Build kubernetes config.
     *
     * @return the kubernetes config
     */
    public KubernetesConfig build() {
      KubernetesConfig kubernetesConfig = new KubernetesConfig();
      kubernetesConfig.setMasterUrl(masterUrl);
      kubernetesConfig.setUsername(username);
      kubernetesConfig.setPassword(password);
      kubernetesConfig.setAccountId(accountId);
      return kubernetesConfig;
    }
  }
}
