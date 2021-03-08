package software.wings.beans.settings.azureartifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.setting.AzureArtifactsYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        azureDevopsUrl, maskingEvaluator));
  }
}
