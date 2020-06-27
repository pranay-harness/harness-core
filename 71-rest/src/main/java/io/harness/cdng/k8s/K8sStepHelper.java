package io.harness.cdng.k8s;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig.K8sClusterConfigBuilder;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

@Singleton
public class K8sStepHelper {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;

  String getReleaseName(Infrastructure infrastructure) {
    switch (infrastructure.getKind()) {
      case K8S_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  SettingAttribute getSettingAttribute(String connectorId) {
    // TODO: change when Connectors NG comes up
    return settingsService.get(connectorId);
  }

  List<EncryptedDataDetail> getEncryptedDataDetails(EncryptableSetting encryptableSetting) {
    // TODO: move to new secret manager apis without app and workflowIds
    return secretManager.getEncryptionDetails(encryptableSetting, "", null);
  }

  K8sClusterConfig getK8sClusterConfig(Infrastructure infrastructure) {
    K8sClusterConfigBuilder k8sClusterConfigBuilder = K8sClusterConfig.builder();

    switch (infrastructure.getKind()) {
      case K8S_DIRECT:
        K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) infrastructure;
        SettingAttribute cloudProvider = getSettingAttribute(k8SDirectInfrastructure.getConnectorId());
        List<EncryptedDataDetail> encryptionDetails =
            getEncryptedDataDetails((KubernetesClusterConfig) cloudProvider.getValue());
        k8sClusterConfigBuilder.cloudProvider(cloudProvider.getValue())
            .namespace(k8SDirectInfrastructure.getNamespace())
            .cloudProviderEncryptionDetails(encryptionDetails)
            .cloudProviderName(cloudProvider.getName());
        return k8sClusterConfigBuilder.build();
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }
}
