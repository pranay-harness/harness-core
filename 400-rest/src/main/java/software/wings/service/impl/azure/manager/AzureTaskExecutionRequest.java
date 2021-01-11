package software.wings.service.impl.azure.manager;

import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.Cd1ApplicationAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.AzureTaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureTaskExecutionRequest
    implements TaskParameters, ExecutionCapabilityDemander, ActivityAccess, Cd1ApplicationAccess {
  private AzureConfigDTO azureConfigDTO;
  private List<EncryptedDataDetail> azureConfigEncryptionDetails;
  private AzureTaskParameters azureTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>(
        CapabilityHelper.generateDelegateCapabilities(azureConfigDTO, azureConfigEncryptionDetails, maskingEvaluator));
    return new ArrayList<>(executionCapabilities);
  }

  @Override
  public String getActivityId() {
    return azureTaskParameters.getActivityId();
  }

  @Override
  public String getAppId() {
    return azureTaskParameters.getAppId();
  }
}
