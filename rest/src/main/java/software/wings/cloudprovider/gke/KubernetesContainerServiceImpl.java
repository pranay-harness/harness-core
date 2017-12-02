package software.wings.cloudprovider.gke;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.awaitility.Awaitility.with;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.apache.commons.lang.StringUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.KubernetesHelperService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 2/9/17
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private static final String RUNNING = "Running";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();

  @Override
  public ReplicationController createController(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, ReplicationController definition) {
    logger.info("Creating controller {}", definition.getMetadata().getName());
    return controllersOperation(kubernetesConfig, encryptedDataDetails).createOrReplace(definition);
  }

  @Override
  public ReplicationController getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    return isNotBlank(name) ? controllersOperation(kubernetesConfig, encryptedDataDetails).withName(name).get() : null;
  }

  @Override
  public ReplicationControllerList getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    return controllersOperation(kubernetesConfig, encryptedDataDetails).withLabels(labels).list();
  }

  @Override
  public ReplicationControllerList listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return controllersOperation(kubernetesConfig, encryptedDataDetails).list();
  }

  @Override
  public void deleteController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    logger.info("Deleting controller {}", name);
    if (isNotBlank(name)) {
      controllersOperation(kubernetesConfig, encryptedDataDetails).withName(name).delete();
    }
  }

  @Override
  public List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String replicationControllerName,
      int previousCount, int count, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(String.format("Resize service [%s] in cluster [%s] from %s to %s instances",
                                              replicationControllerName, clusterName, previousCount, count),
        Log.LogLevel.INFO);
    controllersOperation(kubernetesConfig, encryptedDataDetails).withName(replicationControllerName).scale(count);
    logger.info("Scaled controller {} in cluster {} from {} to {} instances", replicationControllerName, clusterName,
        previousCount, count);
    logger.info("Waiting for pods to be ready...");
    List<Pod> pods = waitForPodsToBeRunning(kubernetesConfig, encryptedDataDetails, replicationControllerName, count);
    List<ContainerInfo> containerInfos = new ArrayList<>();
    boolean hasErrors = false;
    for (Pod pod : pods) {
      String podName = pod.getMetadata().getName();
      ContainerInfo containerInfo = ContainerInfo.builder().build();
      containerInfo.setHostName(podName);
      containerInfo.setContainerId(!pod.getStatus().getContainerStatuses().isEmpty()
              ? StringUtils.substring(pod.getStatus().getContainerStatuses().get(0).getContainerID(), 9, 21)
              : "");
      String phase = pod.getStatus().getPhase();
      if (phase.equals(RUNNING)) {
        containerInfo.setStatus(Status.SUCCESS);
        logger.info("Pod {} started successfully", podName);
        executionLogCallback.saveExecutionLog(String.format("Pod [%s] is running. Host IP: %s. Pod IP: %s", podName,
                                                  pod.getStatus().getHostIP(), pod.getStatus().getPodIP()),
            Log.LogLevel.INFO);
      } else {
        containerInfo.setStatus(Status.FAILURE);
        hasErrors = true;
        String containerMessage =
            Joiner.on("], [").join(pod.getStatus()
                                       .getContainerStatuses()
                                       .stream()
                                       .map(status -> {
                                         ContainerStateWaiting waiting = status.getState().getWaiting();
                                         ContainerStateTerminated terminated = status.getState().getTerminated();
                                         ContainerStateRunning running = status.getState().getRunning();
                                         String msg = status.getName();
                                         if (running != null) {
                                           msg += ": Started at " + running.getStartedAt();
                                         } else if (terminated != null) {
                                           msg += ": " + terminated.getReason() + " - " + terminated.getMessage();
                                         } else if (waiting != null) {
                                           msg += ": " + waiting.getReason() + " - " + waiting.getMessage();
                                         }
                                         return msg;
                                       })
                                       .collect(Collectors.toList()));
        String conditionMessage = Joiner.on("], [").join(pod.getStatus()
                                                             .getConditions()
                                                             .stream()
                                                             .map(cond -> {
                                                               String msg = cond.getType() + ": " + cond.getStatus();
                                                               if (cond.getReason() != null) {
                                                                 msg += " - " + cond.getReason();
                                                               }
                                                               if (cond.getMessage() != null) {
                                                                 msg += " - " + cond.getMessage();
                                                               }
                                                               return msg;
                                                             })
                                                             .collect(Collectors.toList()));
        logger.error("Pod {} failed to start. Current status: {}. Container status: [{}]. Condition: [{}].", podName,
            phase, containerMessage, conditionMessage);
        executionLogCallback.saveExecutionLog(
            String.format("Pod [%s] failed to start. Current status: %s. Container status: [%s]. Condition: [%s].",
                podName, phase, containerMessage, conditionMessage),
            Log.LogLevel.ERROR);
      }
      containerInfos.add(containerInfo);
    }
    if (hasErrors) {
      logger.error("Completed resize operation with errors");
      executionLogCallback.saveExecutionLog("Completed resize operation with errors.", Log.LogLevel.ERROR);
    } else {
      logger.info("Successfully completed resize operation");
      executionLogCallback.saveExecutionLog("Successfully completed resize operation.", Log.LogLevel.INFO);
    }
    return containerInfos;
  }

  @Override
  public int getControllerPodCount(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    return getController(kubernetesConfig, encryptedDataDetails, name).getSpec().getReplicas();
  }

  private NonNamespaceOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      RollableScalableResource<ReplicationController, DoneableReplicationController>>
  controllersOperation(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .replicationControllers()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  @Override
  public Service createOrReplaceService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Service definition) {
    String name = definition.getMetadata().getName();
    Service service = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                          .services()
                          .inNamespace(kubernetesConfig.getNamespace())
                          .withName(name)
                          .get();
    logger.info("{} service [{}]", service == null ? "Creating" : "Replacing", name);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .createOrReplace(definition);
  }

  @Override
  public void createNamespaceIfNotExist(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    NamespaceList namespaces =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails).namespaces().list();
    if (namespaces.getItems().stream().noneMatch(
            namespace -> namespace.getMetadata().getName().equals(kubernetesConfig.getNamespace()))) {
      logger.info("Creating namespace [{}]", kubernetesConfig.getNamespace());
      kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
          .namespaces()
          .create(
              new NamespaceBuilder().withNewMetadata().withName(kubernetesConfig.getNamespace()).endMetadata().build());
    }
  }

  @Override
  public Service getService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    return isNotBlank(name) ? kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                                  .services()
                                  .inNamespace(kubernetesConfig.getNamespace())
                                  .withName(name)
                                  .get()
                            : null;
  }

  @Override
  public ServiceList getServices(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list();
  }

  @Override
  public ServiceList listServices(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .list();
  }

  @Override
  public void deleteService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    logger.info("Deleting service {}", name);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .withName(name)
        .delete();
  }

  @Override
  public Secret getSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String secretName) {
    return isNotBlank(secretName) ? kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                                        .secrets()
                                        .inNamespace(kubernetesConfig.getNamespace())
                                        .withName(secretName)
                                        .get()
                                  : null;
  }

  @Override
  public Secret createOrReplaceSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Secret secret) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .secrets()
        .inNamespace(kubernetesConfig.getNamespace())
        .createOrReplace(secret);
  }

  @Override
  public PodList getPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .pods()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list();
  }

  private List<Pod> waitForPodsToBeRunning(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String replicationControllerName, int number) {
    Map<String, String> labels =
        getController(kubernetesConfig, encryptedDataDetails, replicationControllerName).getMetadata().getLabels();

    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    try {
      with().pollInterval(1, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).until(() -> {
        List<Pod> pods =
            kubernetesClient.pods().inNamespace(kubernetesConfig.getNamespace()).withLabels(labels).list().getItems();
        if (pods.size() != number) {
          return false;
        }
        boolean allRunning = true;
        for (Pod pod : pods) {
          allRunning = allRunning && pod.getStatus().getPhase().equals(RUNNING);
        }
        return allRunning;
      });
    } catch (ConditionTimeoutException e) {
      logger.warn("Timed out waiting for pods to be ready.", e);
    }

    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .pods()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list()
        .getItems();
  }

  public void checkStatus(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String rcName, String serviceName) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    String masterUrl = client.getMasterUrl().toString();
    ReplicationController rc =
        client.replicationControllers().inNamespace(kubernetesConfig.getNamespace()).withName(rcName).get();
    if (rc != null) {
      String rcLink = masterUrl + rc.getMetadata().getSelfLink().substring(1);
      logger.info("Replication controller {}: {}", rcName, rcLink);
    } else {
      logger.info("Replication controller {} does not exist", rcName);
    }
    Service service = client.services().inNamespace(kubernetesConfig.getNamespace()).withName(serviceName).get();
    if (service != null) {
      String serviceLink = masterUrl + service.getMetadata().getSelfLink().substring(1);
      logger.info("Service %s: {}", serviceName, serviceLink);
    } else {
      logger.info("Service {} does not exist", serviceName);
    }
  }

  public void cleanup(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    if (client.services().inNamespace(kubernetesConfig.getNamespace()).list().getItems() != null) {
      client.services().inNamespace(kubernetesConfig.getNamespace()).delete();
      logger.info("Deleted existing services");
    }
    if (client.replicationControllers().inNamespace(kubernetesConfig.getNamespace()).list().getItems() != null) {
      client.replicationControllers().inNamespace(kubernetesConfig.getNamespace()).delete();
      logger.info("Deleted existing replication controllers");
    }
  }
}
