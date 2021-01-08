package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;

public interface K8sDeployRequest extends TaskParameters, ExecutionCapabilityDemander {
  K8sTaskType getTaskType();
  String getCommandName();
  K8sInfraDelegateConfig getK8sInfraDelegateConfig();
  ManifestDelegateConfig getManifestDelegateConfig();
  Integer getTimeoutIntervalInMin();

  @Override
  default List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    K8sInfraDelegateConfig k8sInfraDelegateConfig = getK8sInfraDelegateConfig();
    List<EncryptedDataDetail> cloudProviderEncryptionDetails = k8sInfraDelegateConfig.getEncryptionDataDetails();

    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            cloudProviderEncryptionDetails, maskingEvaluator));

    if (k8sInfraDelegateConfig instanceof DirectK8sInfraDelegateConfig) {
      capabilities.addAll(((DirectK8sInfraDelegateConfig) k8sInfraDelegateConfig)
                              .getKubernetesClusterConfigDTO()
                              .fetchRequiredExecutionCapabilities(maskingEvaluator));
    }
    return capabilities;
  }
}