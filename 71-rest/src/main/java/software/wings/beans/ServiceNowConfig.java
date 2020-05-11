package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.audit.ResourceType.COLLABORATION_PROVIDER;
import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
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
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CollaborationProviderYaml;

import java.util.Arrays;
import java.util.List;

@OwnedBy(CDC)
@JsonTypeName("SERVICENOW")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class ServiceNowConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Base URL", required = true) @NotEmpty private String baseUrl;

  @Attributes(title = "Username", required = true) @NotEmpty private String username;

  /**
   * Handles both password & OAuth(1.0) token.
   */
  @Attributes(title = "Password", required = true) @Encrypted(fieldName = "password") private char[] password;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public ServiceNowConfig(String type) {
    super(type);
  }

  public ServiceNowConfig() {
    super(SettingVariableTypes.SERVICENOW.name());
  }

  public ServiceNowConfig(
      String baseUrl, String username, char[] password, String encryptedPassword, String accountId) {
    this();
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = Arrays.copyOf(password, password.length);
    this.encryptedPassword = encryptedPassword;
    this.accountId = accountId;
  }

  @Override
  public String fetchResourceCategory() {
    return COLLABORATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(baseUrl));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CollaborationProviderYaml {
    private String baseUrl;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;

    @Builder
    public Yaml(String type, String harnessApiVersion, String baseUrl, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.baseUrl = baseUrl;
      this.username = username;
      this.password = password;
    }
  }
}
