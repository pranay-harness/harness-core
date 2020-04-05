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
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by srinivas on 3/30/17.
 */
@JsonTypeName("NEXUS")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class NexusConfig extends SettingValue implements EncryptableSetting, ArtifactSourceable {
  @Attributes(title = "Nexus URL", required = true) @NotEmpty private String nexusUrl;

  @Attributes(title = "Version", required = true, enums = {"2.x", "3.x"})
  @Builder.Default
  private String version = "2.x";

  @Attributes(title = "Username") private String username;

  @Attributes(title = "Password") @Encrypted(fieldName = "password") private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Nexus config.
   */
  public NexusConfig() {
    super(SettingVariableTypes.NEXUS.name());
  }

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public NexusConfig(
      String nexusUrl, String version, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.nexusUrl = nexusUrl;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.version = version;
  }

  @Override
  public String fetchUserName() {
    return username;
  }

  @Override
  public String fetchRegistryUrl() {
    return nexusUrl;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(nexusUrl));
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
