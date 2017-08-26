package software.wings.cloudprovider.gke;

import static org.awaitility.Awaitility.with;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
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
  public ReplicationController createController(KubernetesConfig kubernetesConfig, ReplicationController definition) {
    logger.info("Creating controller {}", definition.getMetadata().getName());
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig)
        .replicationControllers()
        .createOrReplace(definition);
  }

  @Override
  public ReplicationController getController(KubernetesConfig kubernetesConfig, String name) {
    return name != null
        ? kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).get()
        : null;
  }

  @Override
  public ReplicationControllerList listControllers(KubernetesConfig kubernetesConfig) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().list();
  }

  @Override
  public void deleteController(KubernetesConfig kubernetesConfig, String name) {
    logger.info("Deleting controller {}", name);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig).replicationControllers().withName(name).delete();
  }

  @Override
  public List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig, String clusterName,
      String replicationControllerName, int number, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(String.format("Resize service [%s] in cluster [%s] to %s instances",
                                              replicationControllerName, clusterName, number),
        Log.LogLevel.INFO);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig)
        .replicationControllers()
        .withName(replicationControllerName)
        .scale(number);
    logger.info("Scaled controller {} to {} instances", replicationControllerName, number);
    logger.info("Waiting for pods to be ready...");
    List<Pod> pods = waitForPodsToBeRunning(kubernetesConfig, replicationControllerName, number);
    List<ContainerInfo> containerInfos = new ArrayList<>();
    boolean hasErrors = false;
    for (Pod pod : pods) {
      String podName = pod.getMetadata().getName();
      ContainerInfo containerInfo = new ContainerInfo();
      containerInfo.setHostName(podName);
      containerInfo.setContainerId(!pod.getStatus().getContainerStatuses().isEmpty()
              ? StringUtils.substring(pod.getStatus().getContainerStatuses().get(0).getContainerID(), 9, 21)
              : "");
      String phase = pod.getStatus().getPhase();
      if (phase.equals(RUNNING)) {
        containerInfo.setStatus(Status.SUCCESS);
        logger.info("Pod {} started successfully", podName);
        executionLogCallback.saveExecutionLog(String.format("Pod [%s] started successfully. Host IP: %s. Pod IP: %s",
                                                  podName, pod.getStatus().getHostIP(), pod.getStatus().getPodIP()),
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
  public int getControllerPodCount(KubernetesConfig kubernetesConfig, String name) {
    return getController(kubernetesConfig, name).getSpec().getReplicas();
  }

  @Override
  public Service createOrReplaceService(KubernetesConfig kubernetesConfig, Service definition) {
    String name = definition.getMetadata().getName();
    Service service = kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name).get();
    logger.info("{} service [{}]", service == null ? "Creating" : "Replacing", name);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().createOrReplace(definition);
  }

  @Override
  public Service getService(KubernetesConfig kubernetesConfig, String name) {
    return name != null ? kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name).get()
                        : null;
  }

  @Override
  public ServiceList listServices(KubernetesConfig kubernetesConfig) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().list();
  }

  @Override
  public void deleteService(KubernetesConfig kubernetesConfig, String name) {
    logger.info("Deleting service {}", name);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig).services().withName(name).delete();
  }

  @Override
  public Secret getSecret(KubernetesConfig kubernetesConfig, String secretName) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).secrets().withName(secretName).get();
  }

  @Override
  public Secret createOrReplaceSecret(KubernetesConfig kubernetesConfig, Secret secret) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).secrets().createOrReplace(secret);
  }

  private List<Pod> waitForPodsToBeRunning(
      KubernetesConfig kubernetesConfig, String replicationControllerName, int number) {
    Map<String, String> labels = getController(kubernetesConfig, replicationControllerName).getMetadata().getLabels();

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    try {
      with().pollInterval(1, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).until(() -> {
        List<Pod> pods = kubernetesClient.pods().withLabels(labels).list().getItems();
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

    return kubernetesHelperService.getKubernetesClient(kubernetesConfig).pods().withLabels(labels).list().getItems();
  }

  public void checkStatus(KubernetesConfig kubernetesConfig, String rcName, String serviceName) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    String masterUrl = client.getMasterUrl().toString();
    ReplicationController rc = client.replicationControllers().inNamespace("default").withName(rcName).get();
    if (rc != null) {
      String rcLink = masterUrl + rc.getMetadata().getSelfLink().substring(1);
      logger.info("Replication controller {}: {}", rcName, rcLink);
    } else {
      logger.info("Replication controller {} does not exist", rcName);
    }
    Service service = client.services().withName(serviceName).get();
    if (service != null) {
      String serviceLink = masterUrl + service.getMetadata().getSelfLink().substring(1);
      logger.info("Service %s: {}", serviceName, serviceLink);
    } else {
      logger.info("Service {} does not exist", serviceName);
    }
  }

  public void cleanup(KubernetesConfig kubernetesConfig) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    if (client.services().list().getItems() != null) {
      client.services().delete();
      logger.info("Deleted existing services");
    }
    if (client.replicationControllers().list().getItems() != null) {
      client.replicationControllers().delete();
      logger.info("Deleted existing replication controllers");
    }
  }
}
