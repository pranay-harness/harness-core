package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("DOCKER")
public class DockerConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Docker Registry URL", required = true) @NotEmpty private String dockerRegistryUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;
  @Attributes(title = "Encrypted Fields", required = true)
  private final static List<String> encryptedFields = Arrays.asList("password");

  /**
   * Instantiates a new Docker registry config.
   */
  public DockerConfig() {
    super(SettingVariableTypes.DOCKER.name());
  }

  /**
   * Gets the list of fields that are encrypted for use in the UI
   * @return List of field names
   */
  public List<String> getEncryptedFields() {
    return encryptedFields;
  }

  /**
   * Gets docker registry url.
   *
   * @return the docker registry url
   */
  public String getDockerRegistryUrl() {
    return dockerRegistryUrl;
  }

  /**
   * Sets docker registry url.
   *
   * @param dockerRegistryUrl the docker registry url
   */
  public void setDockerRegistryUrl(String dockerRegistryUrl) {
    this.dockerRegistryUrl = dockerRegistryUrl;
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
  public char[] getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
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

    DockerConfig that = (DockerConfig) o;

    if (!dockerRegistryUrl.equals(that.dockerRegistryUrl))
      return false;
    if (!username.equals(that.username))
      return false;
    if (!Arrays.equals(password, that.password))
      return false;
    return accountId.equals(that.accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(dockerRegistryUrl, username, password, accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dockerRegistryUrl", dockerRegistryUrl)
        .add("username", username)
        .add("password", password)
        .add("accountId", accountId)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String dockerRegistryUrl;
    private String type;
    private String username;
    private char[] password;
    private String accountId;

    private Builder() {}

    /**
     * A docker config builder.
     *
     * @return the builder
     */
    public static Builder aDockerConfig() {
      return new Builder();
    }

    /**
     * With docker registry url builder.
     *
     * @param dockerRegistryUrl the docker registry url
     * @return the builder
     */
    public Builder withDockerRegistryUrl(String dockerRegistryUrl) {
      this.dockerRegistryUrl = dockerRegistryUrl;
      return this;
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(String type) {
      this.type = type;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDockerConfig()
          .withDockerRegistryUrl(dockerRegistryUrl)
          .withType(type)
          .withUsername(username)
          .withPassword(password)
          .withAccountId(accountId);
    }

    /**
     * Build docker config.
     *
     * @return the docker config
     */
    public DockerConfig build() {
      DockerConfig dockerConfig = new DockerConfig();
      dockerConfig.setDockerRegistryUrl(dockerRegistryUrl);
      dockerConfig.setType(type);
      dockerConfig.setUsername(username);
      dockerConfig.setPassword(password);
      dockerConfig.setAccountId(accountId);
      return dockerConfig;
    }
  }
}
