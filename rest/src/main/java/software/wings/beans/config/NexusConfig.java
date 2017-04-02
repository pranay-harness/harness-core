package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

/**
 * Created by srinivas on 3/30/17.
 */
@JsonTypeName("NEXUS")
public class NexusConfig extends SettingValue {
  @Attributes(title = "Nexus URL") @URL private String nexusUrl;
  @Attributes(title = "Username") @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class) @Attributes(title = "Password") @NotEmpty private String password;

  /**
   * Instantiates a new Nexus config.
   */
  public NexusConfig() {
    super(SettingVariableTypes.NEXUS.name());
  }

  public String getNexusUrl() {
    return nexusUrl;
  }

  public void setNexusUrl(String nexusUrl) {
    this.nexusUrl = nexusUrl;
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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NexusConfig other = (NexusConfig) o;
    return Objects.equals(this.nexusUrl, other.nexusUrl) && Objects.equals(this.password, other.password)
        && Objects.equals(this.username, other.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nexusUrl, username, password);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nexusUrl", nexusUrl)
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
    private String nexusUrl;

    private Builder() {}

    /**
     * A nexus config builder.
     *
     * @return the builder
     */
    public static NexusConfig.Builder aNexusConfig() {
      return new NexusConfig.Builder();
    }

    /**
     * With password builder.
     *
     * @param password the password
     * @return the builder
     */
    public NexusConfig.Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * With username builder.
     *
     * @param username the username
     * @return the builder
     */
    public NexusConfig.Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    /**
     * With nexus url builder.
     *
     * @param nexusUrl the nexuss url
     * @return the builder
     */
    public NexusConfig.Builder withNexusUrl(String nexusUrl) {
      this.nexusUrl = nexusUrl;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public NexusConfig.Builder but() {
      return aNexusConfig().withPassword(password).withUsername(username).withNexusUrl(nexusUrl);
    }

    /**
     * Build nexus config.
     *
     * @return the nexus config
     */
    public NexusConfig build() {
      NexusConfig NexusConfig = new NexusConfig();
      NexusConfig.setPassword(password);
      NexusConfig.setUsername(username);
      NexusConfig.setNexusUrl(nexusUrl);
      return NexusConfig;
    }
  }
}
