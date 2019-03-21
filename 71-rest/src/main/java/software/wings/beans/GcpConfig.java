package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class GcpConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  private static final String GCS_URL = "https://storage.cloud.google.com/";
  @JsonIgnore @Encrypted private char[] serviceAccountKeyFileContent;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedServiceAccountKeyFileContent;

  public GcpConfig() {
    super(GCP.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public GcpConfig(
      char[] serviceAccountKeyFileContent, String accountId, String encryptedServiceAccountKeyFileContent) {
    this();
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    this.accountId = accountId;
    this.encryptedServiceAccountKeyFileContent = encryptedServiceAccountKeyFileContent;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(GCS_URL));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String serviceAccountKeyFileContent;

    @Builder
    public Yaml(String type, String harnessApiVersion, String serviceAccountKeyFileContent,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    }
  }
}
