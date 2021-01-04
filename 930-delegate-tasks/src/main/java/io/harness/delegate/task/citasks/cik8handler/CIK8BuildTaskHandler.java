package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to setup CI build environment on a K8 cluster including creation of pod as well as image and
 * git secrets.
 */

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.PENDING;
import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.RUNNING;
import static io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder.SECRET;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.k8s.CiK8sTaskResponse;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.CIK8ServicePodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.ci.pod.SecretVarParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.ci.pod.SecretVolumeParams;
import io.harness.delegate.task.citasks.CIBuildTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilder;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CIK8BuildTaskHandler implements CIBuildTaskHandler {
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private CIK8PodSpecBuilder podSpecBuilder;
  @Inject private K8sConnectorHelper k8sConnectorHelper;
  @Inject private SecretSpecBuilder secretSpecBuilder;

  @NotNull private Type type = CIBuildTaskHandler.Type.GCP_K8;

  private static final String DOCKER_CONFIG_KEY = ".dockercfg";
  private static final String HARNESS_IMAGE_SECRET = "HARNESS_IMAGE_SECRET";

  @Override
  public Type getType() {
    return type;
  }

  public K8sTaskExecutionResponse executeTaskInternal(CIBuildSetupTaskParams ciBuildSetupTaskParams) {
    CIK8BuildTaskParams cik8BuildTaskParams = (CIK8BuildTaskParams) ciBuildSetupTaskParams;
    String cik8BuildTaskParamsStr = cik8BuildTaskParams.toString();
    ConnectorDetails gitConnectorDetails = cik8BuildTaskParams.getCik8PodParams().getGitConnector();

    PodParams podParams = cik8BuildTaskParams.getCik8PodParams();
    String namespace = podParams.getNamespace();
    String podName = podParams.getName();

    K8sTaskExecutionResponse result;
    CiK8sTaskResponse k8sTaskResponse = null;
    try (AutoLogContext ignore1 = new K8LogContext(podParams.getName(), null, OVERRIDE_ERROR)) {
      try {
        KubernetesClient kubernetesClient =
            k8sConnectorHelper.createKubernetesClient(cik8BuildTaskParams.getK8sConnector());
        createImageSecrets(kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams);
        createEnvVariablesSecrets(
            kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams, gitConnectorDetails);
        createPVCs(kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams);

        if (cik8BuildTaskParams.getServicePodParams() != null) {
          for (CIK8ServicePodParams servicePodParams : cik8BuildTaskParams.getServicePodParams()) {
            log.info("Creating service for container: {}", servicePodParams);
            createServicePod(kubernetesClient, namespace, servicePodParams);
          }
        }

        Pod pod = podSpecBuilder.createSpec(podParams).build();
        log.info("Creating pod with spec: {}", pod);
        kubeCtlHandler.createPod(kubernetesClient, pod, namespace);
        PodStatus podStatus = kubeCtlHandler.waitUntilPodIsReady(kubernetesClient, podName, namespace);
        k8sTaskResponse = CiK8sTaskResponse.builder().podStatus(podStatus).podName(podName).build();

        boolean isPodRunning = podStatus.getStatus() == RUNNING;
        if (isPodRunning) {
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                       .k8sTaskResponse(k8sTaskResponse)
                       .build();
        } else {
          result = K8sTaskExecutionResponse.builder()
                       .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                       .k8sTaskResponse(k8sTaskResponse)
                       .build();
        }
      } catch (TimeoutException timeoutException) {
        log.error("Processing CI K8 build timed out: {}", cik8BuildTaskParamsStr, timeoutException);
        String errorMessage = k8sTaskResponse.getPodStatus().getErrorMessage();
        k8sTaskResponse.setPodStatus(PodStatus.builder().status(PENDING).errorMessage(errorMessage).build());
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorMessage(timeoutException.getMessage())
                     .k8sTaskResponse(k8sTaskResponse)
                     .build();
      } catch (Exception ex) {
        log.error("Exception in processing CI K8 build setup task: {}", cik8BuildTaskParamsStr, ex);
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorMessage(ex.getMessage())
                     .k8sTaskResponse(k8sTaskResponse)
                     .build();
      }
    }
    return result;
  }

  private void createServicePod(
      KubernetesClient kubernetesClient, String namespace, CIK8ServicePodParams servicePodParams) {
    Pod pod = podSpecBuilder.createSpec((PodParams) servicePodParams.getCik8PodParams()).build();
    log.info("Creating service pod with spec: {}", pod);
    kubeCtlHandler.createPod(kubernetesClient, pod, namespace);

    kubeCtlHandler.createService(kubernetesClient, namespace, servicePodParams.getServiceName(),
        servicePodParams.getSelectorMap(), servicePodParams.getPorts());
  }

  private void createPVCs(
      KubernetesClient kubernetesClient, String namespace, CIK8PodParams<CIK8ContainerParams> podParams) {
    if (podParams.getPvcParamList() == null) {
      return;
    }

    for (PVCParams pvcParams : podParams.getPvcParamList()) {
      if (!pvcParams.isPresent()) {
        log.info("Creating pvc for pod name: {} with name {}", podParams.getName(), pvcParams.getClaimName());
        kubeCtlHandler.createPVC(
            kubernetesClient, namespace, pvcParams.getClaimName(), pvcParams.getStorageClass(), pvcParams.getSizeMib());
      }
    }
  }

  private void createImageSecrets(
      KubernetesClient kubernetesClient, String namespace, CIK8PodParams<CIK8ContainerParams> podParams) {
    log.info("Creating image secrets for pod name: {}", podParams.getName());
    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    Optional.ofNullable(podParams.getContainerParamsList()).ifPresent(containerParamsList::addAll);
    Optional.ofNullable(podParams.getInitContainerParamsList()).ifPresent(containerParamsList::addAll);

    for (CIK8ContainerParams containerParams : containerParamsList) {
      String secretName = format("%s-image-%s", podParams.getName(), containerParams.getName());
      Secret imgSecret = kubeCtlHandler.createRegistrySecret(
          kubernetesClient, namespace, secretName, containerParams.getImageDetailsWithConnector());
      if (imgSecret != null) {
        containerParams.setImageSecret(secretName);
        switch (containerParams.getContainerType()) {
          case SERVICE:
          case PLUGIN:
            updateContainerWithSecretVariable(HARNESS_IMAGE_SECRET,
                SecretParams.builder().type(SecretParams.Type.TEXT).secretKey(DOCKER_CONFIG_KEY).build(), secretName,
                containerParams);
            break;
          default:
            break;
        }
      }
    }
  }

  private void createEnvVariablesSecrets(KubernetesClient kubernetesClient, String namespace,
      CIK8PodParams<CIK8ContainerParams> podParams, ConnectorDetails gitConnectorDetails) {
    log.info("Creating env variables for pod name: {}", podParams.getName());
    List<CIK8ContainerParams> containerParamsList = podParams.getContainerParamsList();
    String secretName = podParams.getName() + "-" + SECRET;

    Map<String, String> secretData = new HashMap<>();
    for (CIK8ContainerParams containerParams : containerParamsList) {
      if (containerParams.getContainerSecrets() == null) {
        continue;
      }

      List<SecretVariableDetails> secretVariableDetails =
          containerParams.getContainerSecrets().getSecretVariableDetails();
      Map<String, ConnectorDetails> connectorDetailsMap =
          containerParams.getContainerSecrets().getConnectorDetailsMap();

      if (isNotEmpty(secretVariableDetails)) {
        Map<String, String> customVarSecretData =
            getAndUpdateCustomVariableSecretData(secretVariableDetails, containerParams, secretName);
        secretData.putAll(customVarSecretData);
      }

      if (isNotEmpty(connectorDetailsMap)) {
        switch (containerParams.getContainerType()) {
          case LITE_ENGINE:
          case PLUGIN:
            Map<String, String> connectorSecretData =
                getAndUpdateConnectorSecretData(connectorDetailsMap, containerParams, secretName);
            secretData.putAll(connectorSecretData);
            break;
          default:
            break;
        }
      }
    }

    Map<String, String> gitSecretData = getAndUpdateGitSecretData(gitConnectorDetails, containerParamsList, secretName);
    secretData.putAll(gitSecretData);

    if (isNotEmpty(secretData)) {
      kubeCtlHandler.createSecret(kubernetesClient, secretName, namespace, secretData);
    }
  }

  private Map<String, String> getAndUpdateConnectorSecretData(
      Map<String, ConnectorDetails> pluginConnectors, CIK8ContainerParams containerParams, String secretName) {
    Map<String, SecretParams> secretData = kubeCtlHandler.fetchConnectorsSecretKeyMap(pluginConnectors);
    if (isNotEmpty(secretData)) {
      updateContainer(containerParams, secretName, secretData);
      return secretData.values().stream().collect(Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    } else {
      return Collections.emptyMap();
    }
  }

  private Map<String, String> getAndUpdateCustomVariableSecretData(
      List<SecretVariableDetails> secretVariableDetails, CIK8ContainerParams containerParams, String secretName) {
    Map<String, SecretParams> customVarSecretData =
        kubeCtlHandler.fetchCustomVariableSecretKeyMap(secretVariableDetails);
    if (isNotEmpty(customVarSecretData)) {
      updateContainer(containerParams, secretName, customVarSecretData);
      return customVarSecretData.values().stream().collect(
          Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    } else {
      return Collections.emptyMap();
    }
  }

  private Map<String, String> getAndUpdateGitSecretData(
      ConnectorDetails gitConnector, List<CIK8ContainerParams> containerParamsList, String secretName) {
    Map<String, SecretParams> gitSecretData = secretSpecBuilder.decryptGitSecretVariables(gitConnector);
    if (isNotEmpty(gitSecretData)) {
      for (CIK8ContainerParams containerParams : containerParamsList) {
        updateContainer(containerParams, secretName, gitSecretData);
      }

      return gitSecretData.values().stream().collect(
          Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    } else {
      return Collections.emptyMap();
    }
  }

  private void updateContainer(
      CIK8ContainerParams containerParams, String secretName, Map<String, SecretParams> secretData) {
    for (Map.Entry<String, SecretParams> secretDataEntry : secretData.entrySet()) {
      switch (secretDataEntry.getValue().getType()) {
        case FILE:
          updateContainerWithSecretVolume(
              secretDataEntry.getKey(), secretDataEntry.getValue(), secretName, containerParams);
          break;
        case TEXT:
          updateContainerWithSecretVariable(
              secretDataEntry.getKey(), secretDataEntry.getValue(), secretName, containerParams);
          break;
        default:
          unhandled(secretDataEntry.getValue().getType());
      }
    }
  }

  private void updateContainerWithSecretVolume(
      String variableName, SecretParams secretParam, String secretName, ContainerParams containerParams) {
    if (secretParam.getType() != SecretParams.Type.FILE) {
      return;
    }
    Map<String, String> envVars = containerParams.getEnvVars();
    if (envVars == null) {
      envVars = new HashMap<>();
      containerParams.setEnvVars(envVars);
    }
    envVars.put(variableName, CIConstants.DEFAULT_SECRET_MOUNT_PATH + secretParam.getSecretKey());

    Map<String, SecretVolumeParams> secretVolumes = containerParams.getSecretVolumes();
    if (secretVolumes == null) {
      secretVolumes = new HashMap<>();
      containerParams.setSecretVolumes(secretVolumes);
    }
    secretVolumes.put(secretParam.getSecretKey(),
        SecretVolumeParams.builder()
            .secretKey(secretParam.getSecretKey())
            .secretName(secretName)
            .mountPath(CIConstants.DEFAULT_SECRET_MOUNT_PATH)
            .build());
  }

  private void updateContainerWithSecretVariable(
      String variableName, SecretParams secretParam, String secretName, ContainerParams containerParams) {
    if (secretParam.getType() != SecretParams.Type.TEXT) {
      return;
    }
    Map<String, SecretVarParams> secretEnvVars = containerParams.getSecretEnvVars();
    if (secretEnvVars == null) {
      secretEnvVars = new HashMap<>();
      containerParams.setSecretEnvVars(secretEnvVars);
    }
    secretEnvVars.put(
        variableName, SecretVarParams.builder().secretKey(secretParam.getSecretKey()).secretName(secretName).build());
  }
}
