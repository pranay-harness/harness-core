package software.wings.beans.config;

import com.google.common.base.MoreObjects;

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
import java.util.Objects;

/**
 * Created by sgurubelli on 6/20/17.
 */
@JsonTypeName("ARTIFACTORY")
public class ArtifactoryConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Artifactory URL", required = true) @NotEmpty private String artifactoryUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @Encrypted
  @NotEmpty
  private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  public ArtifactoryConfig() {
    super(SettingVariableTypes.ARTIFACTORY.name());
  }

  public String getArtifactoryUrl() {
    return artifactoryUrl;
  }

  public void setArtifactoryUrl(String artifactoryUrl) {
    this.artifactoryUrl = artifactoryUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public char[] getPassword() {
    return password;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  @SchemaIgnore
  @Override
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
    ArtifactoryConfig that = (ArtifactoryConfig) o;
    return Objects.equals(artifactoryUrl, that.artifactoryUrl) && Objects.equals(username, that.username)
        && Arrays.equals(password, that.password) && Objects.equals(accountId, that.accountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactoryUrl, username, password, accountId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("artifactoryUrl", artifactoryUrl).add("username", username).toString();
  }

  /**
   * The type Builder
   */
  public static final class Builder {
    private char[] password;
    private String username;
    private String artifactoryUrl;
    private String accountId;

    private Builder() {}

    public static Builder anArtifactoryConfig() {
      return new Builder();
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
     * With artifactory url builder.
     *
     * @param artifactoryUrl the nexus url
     * @return the builder
     */
    public Builder withArtifactoryUrl(String artifactoryUrl) {
      this.artifactoryUrl = artifactoryUrl;
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
      return anArtifactoryConfig()
          .withPassword(password)
          .withUsername(username)
          .withArtifactoryUrl(artifactoryUrl)
          .withAccountId(accountId);
    }

    /**
     * Build nexus config.
     *
     * @return the nexus config
     */
    public ArtifactoryConfig build() {
      ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
      artifactoryConfig.setAccountId(accountId);
      artifactoryConfig.setArtifactoryUrl(artifactoryUrl);
      artifactoryConfig.setPassword(password);
      artifactoryConfig.setUsername(username);
      return artifactoryConfig;
    }
  }
}
