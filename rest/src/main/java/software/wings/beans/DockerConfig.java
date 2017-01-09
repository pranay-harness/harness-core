package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("DOCKER")
public class DockerConfig extends SettingValue {
  @Attributes(title = "Docker Registry URL") private String dockerRegistryUrl;
  private String username;
  private String password;

  /**
   * Instantiates a new Docker registry config.
   */
  public DockerConfig() {
    super(SettingVariableTypes.DOCKER.name());
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String dockerRegistryUrl;
    private String type;
    private String username;
    private String password;

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
      return aDockerConfig()
          .withDockerRegistryUrl(dockerRegistryUrl)
          .withType(type)
          .withUsername(username)
          .withPassword(password);
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
      return dockerConfig;
    }
  }
}
