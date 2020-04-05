package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
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
import software.wings.yaml.setting.VerificationProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * The type Splunk config.
 */
@JsonTypeName("SPLUNK")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class SplunkConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Attributes(title = "URL", required = true) @NotEmpty private String splunkUrl;

  @NotEmpty @Attributes(title = "User Name", required = true) private String username;

  @Attributes(title = "Password", required = true) @Encrypted(fieldName = "password") private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  private SplunkConfig(String splunkUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.splunkUrl = splunkUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(splunkUrl));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String splunkUrl;
    private String username;
    private String password;

    @Builder
    public Yaml(String type, String harnessApiVersion, String splunkUrl, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.splunkUrl = splunkUrl;
      this.username = username;
      this.password = password;
    }
  }
}
