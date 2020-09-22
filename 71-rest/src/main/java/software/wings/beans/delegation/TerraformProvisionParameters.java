package software.wings.beans.delegation;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Value
@Builder
public class TerraformProvisionParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  public static final long TIMEOUT_IN_MINUTES = 100;
  public static final String TERRAFORM = "terraform";

  public enum TerraformCommand { APPLY, DESTROY }

  public enum TerraformCommandUnit {
    Apply,
    Adjust,
    Destroy,
    Rollback;
  }

  private String accountId;
  private final String activityId;
  private final String appId;
  private final String entityId;
  private final String currentStateFileId;
  private final String sourceRepoSettingId;
  private final GitConfig sourceRepo;
  private final String sourceRepoBranch;
  List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private final String scriptPath;
  private final List<NameValuePair> rawVariables;
  @Expression(ALLOW_SECRETS) private final Map<String, String> variables;
  private final Map<String, EncryptedDataDetail> encryptedVariables;

  private final Map<String, String> backendConfigs;
  private final Map<String, EncryptedDataDetail> encryptedBackendConfigs;

  private final TerraformCommand command;
  private final TerraformCommandUnit commandUnit;
  @Builder.Default private long timeoutInMillis = TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES);

  private final List<String> targets;
  private final List<String> tfVarFiles;
  private final boolean runPlanOnly;
  private final boolean exportPlanToApplyStep;
  private final String workspace;
  private final String delegateTag;

  private final byte[] terraformPlan;
  private final boolean saveTerraformJson;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> capabilities =
        CapabilityHelper.generateExecutionCapabilitiesForTerraform(sourceRepoEncryptionDetails);
    if (sourceRepo != null) {
      capabilities.add(GitConnectionCapability.builder()
                           .gitConfig(sourceRepo)
                           .settingAttribute(sourceRepo.getSshSettingAttribute())
                           .encryptedDataDetails(sourceRepoEncryptionDetails)
                           .build());
    }
    return capabilities;
  }
}
