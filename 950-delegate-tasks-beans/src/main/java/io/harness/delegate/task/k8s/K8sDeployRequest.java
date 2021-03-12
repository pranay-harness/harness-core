package io.harness.delegate.task.k8s;

import static io.harness.delegate.beans.storeconfig.StoreDelegateConfigType.HTTP_HELM;
import static io.harness.delegate.task.k8s.ManifestType.HELM_CHART;

import io.harness.delegate.beans.connector.k8Connector.K8sTaskCapabilityHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
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
      capabilities.addAll(K8sTaskCapabilityHelper.fetchRequiredExecutionCapabilities(
          ((DirectK8sInfraDelegateConfig) k8sInfraDelegateConfig).getKubernetesClusterConfigDTO(), maskingEvaluator));
    }

    if (HELM_CHART == getManifestDelegateConfig().getManifestType()) {
      HelmChartManifestDelegateConfig helManifestConfig = (HelmChartManifestDelegateConfig) getManifestDelegateConfig();
      capabilities.add(HelmInstallationCapability.builder()
                           .version(helManifestConfig.getHelmVersion())
                           .criteria(String.format("Helm %s Installed", helManifestConfig.getHelmVersion()))
                           .build());

      if (HTTP_HELM == helManifestConfig.getStoreDelegateConfig().getType()) {
        HttpHelmStoreDelegateConfig httpHelmStoreConfig =
            (HttpHelmStoreDelegateConfig) helManifestConfig.getStoreDelegateConfig();
        capabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            httpHelmStoreConfig.getHttpHelmConnector().getHelmRepoUrl(), maskingEvaluator));
      }
    }

    return capabilities;
  }
}