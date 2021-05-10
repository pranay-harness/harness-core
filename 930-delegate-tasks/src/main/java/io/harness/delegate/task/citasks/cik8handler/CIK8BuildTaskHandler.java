package io.harness.delegate.task.citasks.cik8handler;

/**
 * Delegate task handler to setup CI build environment on a K8 cluster including creation of pod as well as image and
 * git secrets.
 */

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.ci.k8s.PodStatus.Status.RUNNING;
import static io.harness.delegate.beans.ci.pod.CIContainerType.LITE_ENGINE;
import static io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder.getSecretName;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.citasks.CIBuildTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.helper.ProxyVariableHelper;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.delegate.task.citasks.cik8handler.pod.CIK8PodSpecBuilder;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.util.Watch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class CIK8BuildTaskHandler implements CIBuildTaskHandler {
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private CIK8PodSpecBuilder podSpecBuilder;
  @Inject private K8sConnectorHelper k8sConnectorHelper;
  @Inject private SecretSpecBuilder secretSpecBuilder;
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private K8EventHandler k8EventHandler;
  @Inject private ProxyVariableHelper proxyVariableHelper;

  @NotNull private Type type = CIBuildTaskHandler.Type.GCP_K8;

  private static final String DOCKER_CONFIG_KEY = ".dockercfg";
  private static final String HARNESS_IMAGE_SECRET = "HARNESS_IMAGE_SECRET";

  @Override
  public Type getType() {
    return type;
  }

  public K8sTaskExecutionResponse executeTaskInternal(
      CIBuildSetupTaskParams ciBuildSetupTaskParams, ILogStreamingTaskClient logStreamingTaskClient) {
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
        KubernetesConfig kubernetesConfig =
            k8sConnectorHelper.getKubernetesConfig(cik8BuildTaskParams.getK8sConnector());
        KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);

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

        log.info("Setting up pod spec");
        Pod pod = podSpecBuilder.createSpec(podParams).build();
        log.info("Creating pod with spec: {}", pod);
        kubeCtlHandler.createPod(kubernetesClient, pod, namespace);
        Watch<V1Event> watch =
            k8EventHandler.startAsyncPodEventWatch(kubernetesConfig, namespace, podName, logStreamingTaskClient);
        PodStatus podStatus = kubeCtlHandler.waitUntilPodIsReady(kubernetesClient, podName, namespace);
        if (watch != null) {
          k8EventHandler.stopEventWatch(watch);
        }

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
                       .errorMessage(podStatus.getErrorMessage())
                       .k8sTaskResponse(k8sTaskResponse)
                       .build();
        }
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
        log.info("Creating pvc: {} for pod name: {}", pvcParams.getClaimName(), podParams.getName());
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
      log.info("Registry secret creation for pod name {} is complete", podParams.getName());
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
    String secretName = getSecretName(podParams.getName());

    Map<String, String> secretData = new HashMap<>();
    for (CIK8ContainerParams containerParams : containerParamsList) {
      log.info(
          "Creating env variables for container {} present on pod: {}", containerParams.getName(), podParams.getName());

      if (containerParams.getContainerSecrets() == null) {
        continue;
      }

      List<SecretVariableDetails> secretVariableDetails =
          containerParams.getContainerSecrets().getSecretVariableDetails();
      Map<String, ConnectorDetails> connectorDetailsMap =
          containerParams.getContainerSecrets().getConnectorDetailsMap();
      Map<String, ConnectorDetails> functorConnectors = containerParams.getContainerSecrets().getFunctorConnectors();

      if (isNotEmpty(functorConnectors)) {
        log.info("Creating git hub app token env variables for container {} present on pod: {}",
            containerParams.getName(), podParams.getName());
        Map<String, String> githubAppTokenSecretData =
            getAndUpdateGithubAppTokenSecretData(functorConnectors, containerParams, secretName);
        secretData.putAll(githubAppTokenSecretData);
      }
      if (isNotEmpty(secretVariableDetails)) {
        log.info("Creating custom secret env variables for container {} present on pod: {}", containerParams.getName(),
            podParams.getName());
        Map<String, String> customVarSecretData =
            getAndUpdateCustomVariableSecretData(secretVariableDetails, containerParams, secretName);
        secretData.putAll(customVarSecretData);
      }

      if (isNotEmpty(connectorDetailsMap)) {
        log.info("Creating connector env variables for container {} present on pod: {}", containerParams.getName(),
            podParams.getName());
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

      if (containerParams.getContainerType() == LITE_ENGINE) {
        log.info("Creating proxy env variables for container {} present on pod: {}", containerParams.getName(),
            podParams.getName());
        Map<String, String> proxyConfigurationSecretData =
            getAndUpdateProxyConfigurationSecretData(containerParams, secretName);
        secretData.putAll(proxyConfigurationSecretData);
      }
    }

    log.info("Creating git secret env variables for pod: {}", podParams.getName());
    Map<String, String> gitSecretData = getAndUpdateGitSecretData(gitConnectorDetails, containerParamsList, secretName);
    secretData.putAll(gitSecretData);
    log.info("Determined environment secrets to create for stage for pod {}", podParams.getName());

    if (isNotEmpty(secretData)) {
      log.info("Creating environment secrets for pod name: {}", podParams.getName());
      kubeCtlHandler.createSecret(kubernetesClient, secretName, namespace, secretData);
      log.info("Environment k8 secret creation is complete for pod name: {}", podParams.getName());
    }
  }

  private Map<String, String> getAndUpdateGithubAppTokenSecretData(
      Map<String, ConnectorDetails> functorConnectors, CIK8ContainerParams containerParams, String secretName) {
    Map<String, SecretParams> secretData = kubeCtlHandler.fetchGithubAppToken(functorConnectors);
    if (isNotEmpty(secretData)) {
      updateContainer(containerParams, secretName, secretData);
      return secretData.values().stream().collect(Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    } else {
      return Collections.emptyMap();
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

  private Map<String, String> getAndUpdateProxyConfigurationSecretData(
      CIK8ContainerParams containerParams, String secretName) {
    if (proxyVariableHelper.checkIfProxyIsConfigured()) {
      Map<String, SecretParams> proxyConfiguration = proxyVariableHelper.getProxyConfiguration();
      updateContainer(containerParams, secretName, proxyConfiguration);
      return proxyConfiguration.values().stream().collect(
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
