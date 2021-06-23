package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.k8s.CIContainerStatus;
import io.harness.delegate.beans.ci.k8s.PodStatus;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.exception.PodNotFoundException;
import io.harness.threading.Sleeper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * Helper class to interact with K8 cluster for CRUD operation on K8 entities.
 */

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIK8CtlHandler {
  @Inject private SecretSpecBuilder secretSpecBuilder;
  @Inject Provider<ExecCommandListener> execListenerProvider;
  @Inject private Sleeper sleeper;

  private final int MAX_ATTEMPTS = 6;
  private final int DELETION_MAX_ATTEMPTS = 15;

  public Secret createRegistrySecret(
      KubernetesClient kubernetesClient, String namespace, String secretName, ImageDetailsWithConnector imageDetails) {
    Secret secret = secretSpecBuilder.getRegistrySecretSpec(secretName, imageDetails, namespace);
    if (secret == null) {
      return null;
    }

    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying failed to create registry secret; attempt: {}",
        "Failing registry secret creation after retrying {} times");
    return Failsafe.with(retryPolicy)
        .get(() -> kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret));
  }

  public void createPVC(
      KubernetesClient kubernetesClient, String namespace, String claimName, String storageClass, Integer storageMib) {
    List<String> accessModes = new ArrayList<>();
    accessModes.add(CIConstants.PVC_READ_WRITE_ONCE);

    PersistentVolumeClaim pvc =
        new PersistentVolumeClaimBuilder()
            .withNewMetadata()
            .withName(claimName)
            .endMetadata()
            .withNewSpec()
            .withStorageClassName(storageClass)
            .withAccessModes(accessModes)
            .withResources(new ResourceRequirementsBuilder()
                               .addToRequests(CIConstants.STORAGE,
                                   new Quantity(format("%d%s", storageMib, CIConstants.STORAGE_FORMAT)))
                               .build())
            .endSpec()
            .build();
    kubernetesClient.persistentVolumeClaims().inNamespace(namespace).create(pvc);
  }

  // Creates a kubernetes clusterIP service
  public void createService(KubernetesClient kubernetesClient, String namespace, String serviceName,
      Map<String, String> selectorMap, List<Integer> ports) {
    List<ServicePort> svcPorts = new ArrayList<>();
    for (int idx = 0; idx < ports.size(); idx++) {
      String name = String.format("%d-%d", ports.get(idx), idx);
      ServicePort servicePort = new ServicePortBuilder().withName(name).withPort(ports.get(idx)).build();
      svcPorts.add(servicePort);
    }

    Service svc = new ServiceBuilder()
                      .withNewMetadata()
                      .withName(serviceName)
                      .endMetadata()
                      .withNewSpec()
                      .withSelector(selectorMap)
                      .withPorts(svcPorts)
                      .endSpec()
                      .build();
    kubernetesClient.services().inNamespace(namespace).create(svc);
  }

  public Map<String, SecretParams> fetchCustomVariableSecretKeyMap(List<SecretVariableDetails> secretVariableDetails) {
    return secretSpecBuilder.decryptCustomSecretVariables(secretVariableDetails);
  }

  public Map<String, SecretParams> fetchConnectorsSecretKeyMap(Map<String, ConnectorDetails> connectorDetailsMap) {
    return secretSpecBuilder.decryptConnectorSecretVariables(connectorDetailsMap);
  }

  public Map<String, SecretParams> fetchGithubAppToken(Map<String, ConnectorDetails> connectorDetailsMap) {
    return secretSpecBuilder.fetchGithubAppToken(connectorDetailsMap);
  }

  public Map<String, SecretParams> fetchEnvVarsWithSecretRefSecretParams(
      Map<String, String> envVarsWithSecretRef, String containerName) {
    return secretSpecBuilder.createSecretParamsForPlainTextSecret(envVarsWithSecretRef, containerName);
  }

  public Secret createSecret(
      KubernetesClient kubernetesClient, String secretName, String namespace, Map<String, String> data) {
    Secret secret = secretSpecBuilder.createSecret(secretName, namespace, data);

    if (secret != null) {
      RetryPolicy<Object> retryPolicy = getRetryPolicy(
          "[Retrying failed to create secret in attempt: {}", "Failing secret creation after retrying {} times");
      return Failsafe.with(retryPolicy)
          .get(() -> kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret));
    }
    return secret;
  }

  public Pod createPod(KubernetesClient kubernetesClient, Pod pod, String namespace) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying failed pod creation; attempt: {}", "Failed pod creation after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> kubernetesClient.pods().inNamespace(namespace).createOrReplace(pod));
  }

  // Waits for the pod to exit PENDING state and returns true if pod is in RUNNING state, else false.
  public PodStatus waitUntilPodIsReady(KubernetesClient kubernetesClient, String podName, String namespace)
      throws InterruptedException {
    int errorCounter = 0;
    Pod pod = null;
    Instant startTime = Instant.now();
    Instant currTime = startTime;
    while (Duration.between(startTime, currTime).getSeconds() < CIConstants.POD_MAX_WAIT_UNTIL_READY_SECS) {
      // Either pod is in pending phase where it is waiting for scheduling / creation of containers
      // or pod is waiting for containers to move to running state.
      if (pod != null && !isPodInPendingPhase(pod) && !isPodInWaitingState(pod) && isIpAssigned(pod)) {
        return PodStatus.builder()
            .status(PodStatus.Status.RUNNING)
            .ip(pod.getStatus().getPodIP())
            .ciContainerStatusList(getContainersStatus(pod))
            .build();
      }

      sleeper.sleep(CIConstants.POD_WAIT_UNTIL_READY_SLEEP_SECS * 1000L);
      try {
        pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
      } catch (Exception ex) {
        errorCounter = errorCounter + 1;
        log.error("Pod get call failed, errorCounter: {}", errorCounter, ex);
        if (errorCounter >= 5) {
          throw new PodNotFoundException(format("Pod %s is not present in namespace %s", podName, namespace), ex);
        }
        continue;
      }
      currTime = Instant.now();
    }

    String errMsg;
    // If pod's container status list is non-empty, reason for pod not to be in running state is in waiting container's
    // status message. Else reason is present in pod conditions.
    if (Duration.between(startTime, currTime).getSeconds() >= CIConstants.POD_MAX_WAIT_UNTIL_READY_SECS) {
      errMsg = "Timeout exception: Pod containers failed to reach running state within 8 minutes";
    } else if (isNotEmpty(pod.getStatus().getContainerStatuses())) {
      List<String> containerErrs =
          pod.getStatus()
              .getContainerStatuses()
              .stream()
              .filter(containerStatus -> containerStatus.getState().getWaiting() != null)
              .filter(containerStatus -> containerStatus.getState().getWaiting().getMessage() != null)
              .map(containerStatus -> containerStatus.getState().getWaiting().getMessage())
              .collect(Collectors.toList());
      errMsg = String.join(", ", containerErrs);
    } else {
      List<String> podConditions = pod.getStatus()
                                       .getConditions()
                                       .stream()
                                       .filter(Objects::nonNull)
                                       .map(PodCondition::getMessage)
                                       .collect(Collectors.toList());
      errMsg = String.join(", ", podConditions);
    }
    return PodStatus.builder()
        .status(PodStatus.Status.ERROR)
        .errorMessage(errMsg)
        .ciContainerStatusList(getContainersStatus(pod))
        .ip(pod.getStatus().getPodIP())
        .build();
  }

  private boolean isPodInWaitingState(Pod pod) {
    for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
      if (containerStatus.getState().getWaiting() != null) {
        return true;
      }
    }
    return false;
  }

  private boolean isIpAssigned(Pod pod) {
    if (pod.getStatus().getPodIP() != null) {
      return true;
    }
    return false;
  }

  private boolean isPodInPendingPhase(Pod pod) {
    String podPhase = pod.getStatus().getPhase();
    return podPhase.equals(CIConstants.POD_PENDING_PHASE);
  }

  private List<CIContainerStatus> getContainersStatus(Pod pod) {
    List<CIContainerStatus> containerStatusList = new ArrayList<>();
    for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
      String name = containerStatus.getName();
      String image = containerStatus.getImage();
      if (containerStatus.getState().getRunning() != null) {
        containerStatusList.add(CIContainerStatus.builder()
                                    .name(name)
                                    .image(image)
                                    .status(CIContainerStatus.Status.SUCCESS)
                                    .startTime(containerStatus.getState().getRunning().getStartedAt())
                                    .build());
      } else if (containerStatus.getState().getTerminated() != null) {
        ContainerStateTerminated containerStateTerminated = containerStatus.getState().getTerminated();
        containerStatusList.add(CIContainerStatus.builder()
                                    .name(name)
                                    .image(image)
                                    .status(CIContainerStatus.Status.ERROR)
                                    .startTime(containerStateTerminated.getStartedAt())
                                    .endTime(containerStateTerminated.getFinishedAt())
                                    .errorMsg(getContainerErrMsg(
                                        containerStateTerminated.getReason(), containerStateTerminated.getMessage()))
                                    .build());
      } else if (containerStatus.getState().getWaiting() != null) {
        ContainerStateWaiting containerStateWaiting = containerStatus.getState().getWaiting();
        containerStatusList.add(
            CIContainerStatus.builder()
                .name(name)
                .image(image)
                .status(CIContainerStatus.Status.ERROR)
                .errorMsg(getContainerErrMsg(containerStateWaiting.getReason(), containerStateWaiting.getMessage()))
                .build());
      }
    }
    return containerStatusList;
  }

  private String getContainerErrMsg(String reason, String message) {
    if (isEmpty(message)) {
      return reason;
    }

    if (isEmpty(reason)) {
      return message;
    } else {
      return format("%s: %s", reason, message);
    }
  }

  public Boolean deletePod(KubernetesClient kubernetesClient, String podName, String namespace) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicyForDeletion(format("[Retrying failed to delete pod: [%s]; attempt: {}", podName),
            format("Failed to delete pod after retrying {} times", podName));

    return Failsafe.with(retryPolicy)
        .get(() -> kubernetesClient.pods().inNamespace(namespace).withName(podName).delete());
  }

  public Boolean deleteService(KubernetesClient kubernetesClient, String namespace, String serviceName) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicyForDeletion(format("[Retrying failed to delete service: [%s]; attempt: {}", serviceName),
            format("Failed to delete service after retrying {} times", serviceName));

    return Failsafe.with(retryPolicy)
        .get(() -> kubernetesClient.services().inNamespace(namespace).withName(serviceName).delete());
  }

  public Boolean deleteSecret(KubernetesClient kubernetesClient, String namespace, String secretName) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicyForDeletion(format("[Retrying failed to delete secret: [%s]; attempt: {}", secretName),
            format("Failed to delete secret after retrying {} times", secretName));

    return Failsafe.with(retryPolicy)
        .get(() -> kubernetesClient.secrets().inNamespace(namespace).withName(secretName).delete());
  }

  public void createGitSecret(KubernetesClient kubernetesClient, String namespace, ConnectorDetails gitConnector)
      throws UnsupportedEncodingException {
    Secret secret = secretSpecBuilder.getGitSecretSpec(gitConnector, namespace);
    if (secret != null) {
      kubernetesClient.secrets().inNamespace(namespace).createOrReplace(secret);
    }
  }

  /**
   * Executes a command or a list of commands on a container in a pod.
   */
  public K8ExecCommandResponse executeCommand(KubernetesClient kubernetesClient, String podName, String containerName,
      String namespace, String[] commands, Integer timeoutSecs) throws InterruptedException {
    ExecCommandListener execListener = execListenerProvider.get();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (ExecWatch watch = kubernetesClient.pods()
                               .inNamespace(namespace)
                               .withName(podName)
                               .inContainer(containerName)
                               .writingOutput(outputStream)
                               .usingListener(execListener)
                               .exec(commands)) {
      ExecCommandStatus execCommandStatus = getCmdExecutionStatus(execListener, timeoutSecs);
      return K8ExecCommandResponse.builder().outputStream(outputStream).execCommandStatus(execCommandStatus).build();
    }
  }

  private ExecCommandStatus getCmdExecutionStatus(ExecCommandListener execCommandListener, Integer timeoutSecs)
      throws InterruptedException {
    ExecCommandStatus execCommandStatus;
    try {
      boolean isCommandCompleted = execCommandListener.isCommandExecutionComplete(timeoutSecs);
      if (isCommandCompleted) {
        execCommandStatus = ExecCommandStatus.SUCCESS;
      } else {
        execCommandStatus = ExecCommandStatus.ERROR;
      }
    } catch (TimeoutException e) {
      log.warn("Failed to execute command with error: ", e);
      execCommandStatus = ExecCommandStatus.TIMEOUT;
    }
    return execCommandStatus;
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withBackoff(2, 10, ChronoUnit.SECONDS)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }

  private RetryPolicy<Object> getRetryPolicyForDeletion(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withMaxAttempts(DELETION_MAX_ATTEMPTS)
        .withBackoff(5, 60, ChronoUnit.SECONDS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
