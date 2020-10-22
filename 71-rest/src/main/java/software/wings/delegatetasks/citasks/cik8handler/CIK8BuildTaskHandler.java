package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Delegate task handler to setup CI build environment on a K8 cluster including creation of pod as well as image and
 * git secrets.
 */

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.delegatetasks.citasks.cik8handler.SecretSpecBuilder.SECRET;
import static software.wings.delegatetasks.citasks.cik8handler.params.CIConstants.DEFAULT_SECRET_MOUNT_PATH;
import static software.wings.helpers.ext.k8s.response.PodStatus.Status.PENDING;
import static software.wings.helpers.ext.k8s.response.PodStatus.Status.RUNNING;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.ImageDetails;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.ci.CIBuildSetupTaskParams;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.CIK8ServicePodParams;
import software.wings.beans.ci.pod.ContainerParams;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.PVCParams;
import software.wings.beans.ci.pod.PodParams;
import software.wings.beans.ci.pod.SecretParams;
import software.wings.beans.ci.pod.SecretVarParams;
import software.wings.beans.ci.pod.SecretVolumeParams;
import software.wings.delegatetasks.citasks.CIBuildTaskHandler;
import software.wings.delegatetasks.citasks.cik8handler.pod.CIK8PodSpecBuilder;
import software.wings.helpers.ext.k8s.response.CiK8sTaskResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.PodStatus;
import software.wings.service.intfc.security.EncryptionService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class CIK8BuildTaskHandler implements CIBuildTaskHandler {
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private CIK8CtlHandler kubeCtlHandler;
  @Inject private CIK8PodSpecBuilder podSpecBuilder;
  @Inject private EncryptionService encryptionService;
  @NotNull private Type type = CIBuildTaskHandler.Type.GCP_K8;

  private static final String imageIdFormat = "%s-%s";

  @Override
  public Type getType() {
    return type;
  }

  public K8sTaskExecutionResponse executeTaskInternal(CIBuildSetupTaskParams ciBuildSetupTaskParams) {
    CiK8sTaskResponse k8sTaskResponse = null;
    CIK8BuildTaskParams cik8BuildTaskParams = (CIK8BuildTaskParams) ciBuildSetupTaskParams;
    GitFetchFilesConfig gitFetchFilesConfig = cik8BuildTaskParams.getGitFetchFilesConfig();

    PodParams podParams = cik8BuildTaskParams.getCik8PodParams();
    String namespace = podParams.getNamespace();
    String podName = podParams.getName();

    K8sTaskExecutionResponse result;
    try (AutoLogContext ignore1 = new K8LogContext(podParams.getName(), null, OVERRIDE_ERROR)) {
      try {
        KubernetesClient kubernetesClient = createKubernetesClient(cik8BuildTaskParams);
        createGitSecret(kubernetesClient, namespace, gitFetchFilesConfig);
        createImageSecrets(kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams);
        createEnvVariablesSecrets(kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams);
        createPVCs(kubernetesClient, namespace, (CIK8PodParams<CIK8ContainerParams>) podParams);

        if (cik8BuildTaskParams.getServicePodParams() != null) {
          for (CIK8ServicePodParams servicePodParams : cik8BuildTaskParams.getServicePodParams()) {
            logger.info("Creating service for container: {}", servicePodParams);
            createServicePod(kubernetesClient, namespace, servicePodParams);
          }
        }
        Pod pod = podSpecBuilder.createSpec(podParams).build();
        logger.info("Creating pod with spec: {}", pod);
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
        logger.error("Processing CI K8 build timed out: {}", ciBuildSetupTaskParams, timeoutException);
        String errorMessage = k8sTaskResponse.getPodStatus().getErrorMessage();
        k8sTaskResponse.setPodStatus(PodStatus.builder().status(PENDING).errorMessage(errorMessage).build());
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorMessage(timeoutException.getMessage())
                     .k8sTaskResponse(k8sTaskResponse)
                     .build();
      } catch (Exception ex) {
        logger.error("Exception in processing CI K8 build setup task: {}", ciBuildSetupTaskParams, ex);
        result = K8sTaskExecutionResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorMessage(ex.getMessage())
                     .k8sTaskResponse(k8sTaskResponse)
                     .build();
      }
    }
    return result;
  }

  private KubernetesClient createKubernetesClient(CIK8BuildTaskParams cik8BuildTaskParams) {
    encryptionService.decrypt(
        cik8BuildTaskParams.getKubernetesClusterConfig(), cik8BuildTaskParams.getEncryptionDetails(), false);
    KubernetesConfig kubernetesConfig = cik8BuildTaskParams.getKubernetesClusterConfig().createKubernetesConfig(null);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig);
  }

  private void createServicePod(
      KubernetesClient kubernetesClient, String namespace, CIK8ServicePodParams servicePodParams) {
    Pod pod = podSpecBuilder.createSpec((PodParams) servicePodParams.getCik8PodParams()).build();
    logger.info("Creating service pod with spec: {}", pod);
    kubeCtlHandler.createPod(kubernetesClient, pod, namespace);

    kubeCtlHandler.createService(kubernetesClient, namespace, servicePodParams.getServiceName(),
        servicePodParams.getSelectorMap(), servicePodParams.getPorts());
  }

  private void createGitSecret(
      KubernetesClient kubernetesClient, String namespace, GitFetchFilesConfig gitFetchFilesConfig) {
    if (gitFetchFilesConfig == null) {
      return;
    }
    logger.info("Creating git secret in namespace: {} for accountId: {}, ", namespace,
        gitFetchFilesConfig.getGitConfig().getAccountId());
    try {
      kubeCtlHandler.createGitSecret(kubernetesClient, namespace, gitFetchFilesConfig.getGitConfig(),
          gitFetchFilesConfig.getEncryptedDataDetails());
    } catch (UnsupportedEncodingException e) {
      String errMsg = format("Unknown format for GIT password %s", e.getMessage());
      logger.error(errMsg);
      throw new InvalidRequestException(errMsg, e, WingsException.USER);
    }
  }

  private void createPVCs(
      KubernetesClient kubernetesClient, String namespace, CIK8PodParams<CIK8ContainerParams> podParams) {
    logger.info("Creating pvc for pod name: {}", podParams.getName());
    if (podParams.getPvcParamList() == null) {
      return;
    }

    for (PVCParams pvcParams : podParams.getPvcParamList()) {
      if (!pvcParams.isPresent()) {
        kubeCtlHandler.createPVC(
            kubernetesClient, namespace, pvcParams.getClaimName(), pvcParams.getStorageClass(), pvcParams.getSizeMib());
      }
    }
  }

  private void createImageSecrets(
      KubernetesClient kubernetesClient, String namespace, CIK8PodParams<CIK8ContainerParams> podParams) {
    logger.info("Creating image secrets for pod name: {}", podParams.getName());
    List<CIK8ContainerParams> containerParamsList = new ArrayList<>();
    Optional.ofNullable(podParams.getContainerParamsList()).ifPresent(containerParamsList::addAll);
    Optional.ofNullable(podParams.getInitContainerParamsList()).ifPresent(containerParamsList::addAll);

    Map<String, ImageDetailsWithConnector> imageDetailsById = new HashMap<>();
    for (CIK8ContainerParams containerParams : containerParamsList) {
      ImageDetails imageDetails = containerParams.getImageDetailsWithConnector().getImageDetails();
      if (isNotBlank(imageDetails.getRegistryUrl())) {
        imageDetailsById.put(format(imageIdFormat, imageDetails.getName(), imageDetails.getRegistryUrl()),
            containerParams.getImageDetailsWithConnector());
      }
    }
    imageDetailsById.forEach(
        (imageId, imageDetails) -> kubeCtlHandler.createRegistrySecret(kubernetesClient, namespace, imageDetails));
  }

  private void createEnvVariablesSecrets(
      KubernetesClient kubernetesClient, String namespace, CIK8PodParams<CIK8ContainerParams> podParams) {
    logger.info("Creating env variables for pod name: {}", podParams.getName());
    List<CIK8ContainerParams> containerParamsList = podParams.getContainerParamsList();
    String secretName = podParams.getName() + "-" + SECRET;

    Map<String, String> secretData = new HashMap<>();
    for (CIK8ContainerParams containerParams : containerParamsList) {
      if (containerParams.getContainerSecrets() == null) {
        continue;
      }

      Map<String, EncryptedVariableWithType> encryptedSecrets =
          containerParams.getContainerSecrets().getEncryptedSecrets();
      Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues =
          containerParams.getContainerSecrets().getPublishArtifactEncryptedValues();

      if (isNotEmpty(encryptedSecrets)) {
        Map<String, String> customVarSecretData =
            getAndUpdateCustomVariableSecretData(encryptedSecrets, containerParams, secretName);
        secretData.putAll(customVarSecretData);
      }

      if (isNotEmpty(containerParams.getContainerSecrets().getPublishArtifactEncryptedValues())
          && containerParams.getContainerType() == CIContainerType.LITE_ENGINE) {
        Map<String, String> publishArtifactSecretData =
            getAndUpdatePublishArtifactSecretData(publishArtifactEncryptedValues, containerParams, secretName);
        secretData.putAll(publishArtifactSecretData);
      }
    }

    kubeCtlHandler.createSecret(kubernetesClient, secretName, namespace, secretData);
  }

  private Map<String, String> getAndUpdateCustomVariableSecretData(
      Map<String, EncryptedVariableWithType> encryptedSecrets, CIK8ContainerParams containerParams, String secretName) {
    Map<String, SecretParams> customVarSecretData = kubeCtlHandler.fetchCustomVariableSecretKeyMap(encryptedSecrets);
    if (!isEmpty(customVarSecretData)) {
      updateContainer(containerParams, secretName, customVarSecretData);
      return customVarSecretData.values().stream().collect(
          Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
    } else {
      return Collections.emptyMap();
    }
  }

  private Map<String, String> getAndUpdatePublishArtifactSecretData(
      Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues,
      CIK8ContainerParams containerParams, String secretName) {
    Map<String, SecretParams> secretData =
        kubeCtlHandler.fetchPublishArtifactSecretKeyMap(publishArtifactEncryptedValues);
    if (!isEmpty(secretData)) {
      updateContainer(containerParams, secretName, secretData);
      return secretData.values().stream().collect(Collectors.toMap(SecretParams::getSecretKey, SecretParams::getValue));
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
    envVars.put(variableName, DEFAULT_SECRET_MOUNT_PATH + secretParam.getSecretKey());

    Map<String, SecretVolumeParams> secretVolumes = containerParams.getSecretVolumes();
    if (secretVolumes == null) {
      secretVolumes = new HashMap<>();
      containerParams.setSecretVolumes(secretVolumes);
    }
    secretVolumes.put(secretParam.getSecretKey(),
        SecretVolumeParams.builder()
            .secretKey(secretParam.getSecretKey())
            .secretName(secretName)
            .mountPath(DEFAULT_SECRET_MOUNT_PATH)
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