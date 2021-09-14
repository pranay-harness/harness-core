/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType.MANUAL_CREDENTIALS;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.task.gcp.helpers.GkeClusterHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
@OwnedBy(CDP)
public class ContainerDeploymentDelegateBaseHelper {
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private GkeClusterHelper gkeClusterHelper;

  @NotNull
  public List<Pod> getExistingPodsByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    return emptyIfNull(kubernetesContainerService.getPods(kubernetesConfig, labels));
  }

  private List<ContainerInfo> fetchContainersUsingControllersWhenReady(KubernetesConfig kubernetesConfig,
      LogCallback executionLogCallback, List<? extends HasMetadata> controllers, List<Pod> existingPods) {
    if (isNotEmpty(controllers)) {
      return controllers.stream()
          .filter(controller
              -> !(controller.getKind().equals("ReplicaSet") && controller.getMetadata().getOwnerReferences() != null))
          .flatMap(controller -> {
            boolean isNotVersioned =
                controller.getKind().equals("DaemonSet") || controller.getKind().equals("StatefulSet");
            return kubernetesContainerService
                .getContainerInfosWhenReady(kubernetesConfig, controller.getMetadata().getName(), 0, -1,
                    (int) TimeUnit.MINUTES.toMinutes(30), existingPods, isNotVersioned, executionLogCallback, true, 0,
                    kubernetesConfig.getNamespace())
                .stream();
          })
          .collect(Collectors.toList());
    }
    return emptyList();
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(
      KubernetesConfig kubernetesConfig, LogCallback logCallback, Map<String, String> labels, List<Pod> existingPods) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    logCallback.saveExecutionLog(format("Deployed Controllers [%s]:", controllers.size()));
    controllers.forEach(controller
        -> logCallback.saveExecutionLog(String.format("Kind:%s, Name:%s (desired: %s)", controller.getKind(),
            controller.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(controller))));

    return fetchContainersUsingControllersWhenReady(kubernetesConfig, logCallback, controllers, existingPods);
  }

  public int getControllerCountByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    return controllers.size();
  }

  public KubernetesConfig createKubernetesConfig(K8sInfraDelegateConfig clusterConfigDTO) {
    if (clusterConfigDTO instanceof DirectK8sInfraDelegateConfig) {
      return k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(
          ((DirectK8sInfraDelegateConfig) clusterConfigDTO).getKubernetesClusterConfigDTO(),
          clusterConfigDTO.getNamespace());
    } else if (clusterConfigDTO instanceof GcpK8sInfraDelegateConfig) {
      GcpK8sInfraDelegateConfig gcpK8sInfraDelegateConfig = (GcpK8sInfraDelegateConfig) clusterConfigDTO;
      GcpConnectorCredentialDTO gcpCredentials = gcpK8sInfraDelegateConfig.getGcpConnectorDTO().getCredential();
      return gkeClusterHelper.getCluster(getGcpServiceAccountKeyFileContent(gcpCredentials),
          gcpCredentials.getGcpCredentialType() == INHERIT_FROM_DELEGATE, gcpK8sInfraDelegateConfig.getCluster(),
          gcpK8sInfraDelegateConfig.getNamespace());
    } else {
      throw new InvalidRequestException("Unhandled K8sInfraDelegateConfig " + clusterConfigDTO.getClass());
    }
  }

  private char[] getGcpServiceAccountKeyFileContent(GcpConnectorCredentialDTO gcpCredentials) {
    if (gcpCredentials.getGcpCredentialType() == MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO gcpCredentialSpecDTO = (GcpManualDetailsDTO) gcpCredentials.getConfig();
      return gcpCredentialSpecDTO.getSecretKeyRef().getDecryptedValue();
    }
    return null;
  }

  public String getKubeconfigFileContent(K8sInfraDelegateConfig k8sInfraDelegateConfig) {
    decryptK8sInfraDelegateConfig(k8sInfraDelegateConfig);
    return kubernetesContainerService.getConfigFileContent(createKubernetesConfig(k8sInfraDelegateConfig));
  }

  public void decryptK8sInfraDelegateConfig(K8sInfraDelegateConfig k8sInfraDelegateConfig) {
    if (k8sInfraDelegateConfig instanceof DirectK8sInfraDelegateConfig) {
      DirectK8sInfraDelegateConfig directK8sInfraDelegateConfig = (DirectK8sInfraDelegateConfig) k8sInfraDelegateConfig;
      decryptK8sClusterConfig(directK8sInfraDelegateConfig.getKubernetesClusterConfigDTO(),
          directK8sInfraDelegateConfig.getEncryptionDataDetails());
    } else if (k8sInfraDelegateConfig instanceof GcpK8sInfraDelegateConfig) {
      GcpK8sInfraDelegateConfig k8sGcpInfraDelegateConfig = (GcpK8sInfraDelegateConfig) k8sInfraDelegateConfig;
      decryptGcpClusterConfig(
          k8sGcpInfraDelegateConfig.getGcpConnectorDTO(), k8sGcpInfraDelegateConfig.getEncryptionDataDetails());
    }
  }

  public void decryptK8sClusterConfig(
      KubernetesClusterConfigDTO clusterConfigDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    if (clusterConfigDTO.getCredential().getKubernetesCredentialType() == KubernetesCredentialType.MANUAL_CREDENTIALS) {
      KubernetesClusterDetailsDTO clusterDetailsDTO =
          (KubernetesClusterDetailsDTO) clusterConfigDTO.getCredential().getConfig();
      KubernetesAuthCredentialDTO authCredentialDTO = clusterDetailsDTO.getAuth().getCredentials();
      secretDecryptionService.decrypt(authCredentialDTO, encryptedDataDetails);
    }
  }

  public void decryptGcpClusterConfig(GcpConnectorDTO gcpConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    if (gcpConnectorDTO.getCredential().getGcpCredentialType() == MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO gcpCredentialSpecDTO = (GcpManualDetailsDTO) gcpConnectorDTO.getCredential().getConfig();
      secretDecryptionService.decrypt(gcpCredentialSpecDTO, encryptedDataDetails);
    }
  }
}
