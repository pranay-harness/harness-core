package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonTypeName("INSTANA")
@Data
@Builder
@ToString(exclude = "apiToken")
@EqualsAndHashCode(callSuper = false)
public class InstanaConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @SchemaIgnore @NotEmpty private String accountId;
  @NotEmpty private String instanaUrl;
  @Encrypted private char apiToken[];
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiToken;

  public InstanaConfig() {
    super(SettingVariableTypes.INSTANA.name());
  }
  // this is needed for Builder to work.
  private InstanaConfig(String accountId, String instanaUrl, char apiToken[], String encryptedApiToken) {
    this();
    this.accountId = accountId;
    this.instanaUrl = instanaUrl;
    this.apiToken = apiToken;
    this.encryptedApiToken = encryptedApiToken;
  }
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(instanaUrl));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String instanaUrl;
    private String apiToken;
    @Builder
    public Yaml(String type, String harnessApiVersion, String instanaUrl, String apiToken,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.instanaUrl = instanaUrl;
      this.apiToken = apiToken;
    }
  }
}