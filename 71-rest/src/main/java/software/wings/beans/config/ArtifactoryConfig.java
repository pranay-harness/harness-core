package software.wings.beans.config;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by sgurubelli on 6/20/17.
 */
@JsonTypeName("ARTIFACTORY")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class ArtifactoryConfig extends SettingValue implements EncryptableSetting, ArtifactSourceable {
  @Attributes(title = "Artifactory URL", required = true) @NotEmpty private String artifactoryUrl;

  @Attributes(title = "Username") private String username;

  @Attributes(title = "Password") @Encrypted(fieldName = "password") private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public ArtifactoryConfig() {
    super(SettingVariableTypes.ARTIFACTORY.name());
  }

  public ArtifactoryConfig(
      String artifactoryUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.artifactoryUrl = artifactoryUrl;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  // NOTE: Do not remove this. As UI expects this field should be there..Lombok Default is not working
  public String getUsername() {
    return Objects.isNull(username) ? "" : username;
  }

  @Override
  public String fetchUserName() {
    return username;
  }

  @Override
  public String fetchRegistryUrl() {
    return artifactoryUrl;
  }

  @SchemaIgnore
  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getArtifactoryUrl()));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
    }
  }
}
