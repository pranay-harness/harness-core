package software.wings.beans.settings.helm;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmRepoConfigValidationTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private HelmRepoConfig helmRepoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoDisplayName;

  private SettingValue connectorConfig;
  private List<EncryptedDataDetail> connectorEncryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CapabilityHelper.generateDelegateCapabilities(helmRepoConfig, encryptedDataDetails, maskingEvaluator);
  }
}
