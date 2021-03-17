package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class K8sTaskCapabilityHelper {
  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      KubernetesClusterConfigDTO kubernetesClusterConfigDTO, ExpressionEvaluator maskingEvaluator) {
    KubernetesCredentialDTO credential = kubernetesClusterConfigDTO.getCredential();
    if (credential.getKubernetesCredentialType() == KubernetesCredentialType.INHERIT_FROM_DELEGATE) {
      KubernetesDelegateDetailsDTO k8sDetails = (KubernetesDelegateDetailsDTO) credential.getConfig();
      return Collections.singletonList(
          SelectorCapability.builder().selectors(k8sDetails.getDelegateSelectors()).build());
    } else if (credential.getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO k8sManualCreds = (KubernetesClusterDetailsDTO) credential.getConfig();
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
              k8sManualCreds.getMasterUrl(), maskingEvaluator));
    } else {
      throw new UnknownEnumTypeException(
          "Kubernetes Credential Type", String.valueOf(credential.getKubernetesCredentialType()));
    }
  }
}
