package software.wings.beans.settings.azureartifacts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
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
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.AzureArtifactsYaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonTypeName("AZURE_ARTIFACTS_PAT")
@Data
@Builder
@ToString(exclude = {"pat"})
@EqualsAndHashCode(callSuper = false)
public class AzureArtifactsPATConfig extends SettingValue implements AzureArtifactsConfig {
  @SchemaIgnore @NotEmpty private String accountId;

  @NotEmpty private String azureDevopsUrl;
  @Encrypted(fieldName = "pat") private char[] pat;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPat;

  private AzureArtifactsPATConfig() {
    super(SettingVariableTypes.AZURE_ARTIFACTS_PAT.name());
  }

  public AzureArtifactsPATConfig(String accountId, String azureDevopsUrl, final char[] pat, String encryptedPat) {
    this();
    this.accountId = accountId;
    this.azureDevopsUrl = azureDevopsUrl;
    this.pat = (pat != null) ? Arrays.copyOf(pat, pat.length) : null;
    this.encryptedPat = encryptedPat;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(azureDevopsUrl));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends AzureArtifactsYaml {
    private String azureDevopsUrl;
    private String pat;

    @Builder
    public Yaml(String type, String harnessApiVersion, String azureDevopsUrl, String pat,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.azureDevopsUrl = azureDevopsUrl;
      this.pat = pat;
    }
  }
}
