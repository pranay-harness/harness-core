package software.wings.beans.config;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.setting.ArtifactServerYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by srinivas on 3/30/17.
 */
@OwnedBy(CDC)
@JsonTypeName("NEXUS")
@Data
@Builder
@ToString(exclude = {"password"})
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

  @SchemaIgnore @Transient private boolean useCredentialsWithAuth;
  /**
   * Instantiates a new Nexus config.
   */
  public NexusConfig() {
    super(SettingVariableTypes.NEXUS.name());
  }

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public NexusConfig(String nexusUrl, String version, String username, char[] password, String accountId,
      String encryptedPassword, boolean useCredentialsWithAuth) {
    this();
    this.nexusUrl = nexusUrl;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.version = version;
    this.useCredentialsWithAuth = useCredentialsWithAuth;
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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(nexusUrl, maskingEvaluator));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    private String version;
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password, String version,
        UsageRestrictionYaml usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.version = version;
    }
  }
}
