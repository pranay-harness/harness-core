package software.wings.cloudprovider.gke;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.CrossVersionObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetList;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentList;
import io.fabric8.kubernetes.api.model.extensions.DoneableDaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DoneableDeployment;
import io.fabric8.kubernetes.api.model.extensions.DoneableReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetList;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.client.IstioClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ContainerApiVersions;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.ContainerInfoBuilder;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.KubernetesHelperService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by brett on 2/9/17
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesContainerServiceImpl.class);

  private static final String RUNNING = "Running";

  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();
  @Inject private TimeLimiter timeLimiter;

  @Override
  public HasMetadata createController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, HasMetadata definition) {
    logger.info("Creating {} {}", definition.getKind(), definition.getMetadata().getName());

    // TODO - Use definition.getKind()
    HasMetadata controller = null;
    if (definition instanceof ReplicationController) {
      controller =
          rcOperations(kubernetesConfig, encryptedDataDetails).createOrReplace((ReplicationController) definition);
    } else if (definition instanceof Deployment) {
      controller =
          deploymentOperations(kubernetesConfig, encryptedDataDetails).createOrReplace((Deployment) definition);
    } else if (definition instanceof ReplicaSet) {
      controller = replicaOperations(kubernetesConfig, encryptedDataDetails).createOrReplace((ReplicaSet) definition);
    } else if (definition instanceof StatefulSet) {
      controller = statefulOperations(kubernetesConfig, encryptedDataDetails).createOrReplace((StatefulSet) definition);
    } else if (definition instanceof DaemonSet) {
      controller = daemonOperations(kubernetesConfig, encryptedDataDetails).createOrReplace((DaemonSet) definition);
    }
    return controller;
  }

  @Override
  public HasMetadata getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    HasMetadata controller = null;
    if (isNotBlank(name)) {
      controller = rcOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
      if (controller == null) {
        controller = deploymentOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
      }
      if (controller == null) {
        controller = replicaOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
      }
      if (controller == null) {
        controller = statefulOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
      }
      if (controller == null) {
        controller = daemonOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
      }
    }
    return controller;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<? extends HasMetadata> getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = new ArrayList<>();
    controllers.addAll(
        (List) rcOperations(kubernetesConfig, encryptedDataDetails).withLabels(labels).list().getItems());
    controllers.addAll(
        (List) deploymentOperations(kubernetesConfig, encryptedDataDetails).withLabels(labels).list().getItems());
    controllers.addAll(
        (List) replicaOperations(kubernetesConfig, encryptedDataDetails).withLabels(labels).list().getItems());
    controllers.addAll(
        (List) statefulOperations(kubernetesConfig, encryptedDataDetails).withLabels(labels).list().getItems());
    controllers.addAll(
        (List) daemonOperations(kubernetesConfig, encryptedDataDetails).withLabels(labels).list().getItems());
    return controllers;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<? extends HasMetadata> listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    List<? extends HasMetadata> controllers = new ArrayList<>();
    controllers.addAll((List) rcOperations(kubernetesConfig, encryptedDataDetails).list().getItems());
    controllers.addAll((List) deploymentOperations(kubernetesConfig, encryptedDataDetails).list().getItems());
    controllers.addAll((List) replicaOperations(kubernetesConfig, encryptedDataDetails).list().getItems());
    controllers.addAll((List) statefulOperations(kubernetesConfig, encryptedDataDetails).list().getItems());
    controllers.addAll((List) daemonOperations(kubernetesConfig, encryptedDataDetails).list().getItems());
    return controllers;
  }

  @Override
  public void deleteController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    logger.info("Deleting controller {}", name);
    if (isNotBlank(name)) {
      HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, name);
      if (controller instanceof ReplicationController) {
        rcOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
      } else if (controller instanceof Deployment) {
        deploymentOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
      } else if (controller instanceof ReplicaSet) {
        replicaOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
      } else if (controller instanceof StatefulSet) {
        statefulOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
      } else if (controller instanceof DaemonSet) {
        daemonOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
      }
    }
  }

  @Override
  public HorizontalPodAutoscaler createAutoscaler(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, HorizontalPodAutoscaler definition) {
    String api = kubernetesHelperService.trimVersion(definition.getApiVersion());

    if (ContainerApiVersions.KUBERNETES_V1.getVersionName().equals(api)) {
      return kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).createOrReplace(definition);
    } else {
      return kubernetesHelperService.hpaOperationsForCustomMetricHPA(kubernetesConfig, encryptedDataDetails, api)
          .createOrReplace(definition);
    }
  }

  @Override
  public HorizontalPodAutoscaler getAutoscaler(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String name, String apiVersion) {
    if (ContainerApiVersions.KUBERNETES_V1.getVersionName().equals(apiVersion) || StringUtils.isEmpty(apiVersion)) {
      return kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
    } else {
      return kubernetesHelperService.hpaOperationsForCustomMetricHPA(kubernetesConfig, encryptedDataDetails, apiVersion)
          .withName(name)
          .get();
    }
  }

  @Override
  public List<HorizontalPodAutoscaler> listAutoscalers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).list().getItems();
  }

  @Override
  public void disableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, String apiVersion) {
    HorizontalPodAutoscaler autoscaler = getAutoscaler(kubernetesConfig, encryptedDataDetails, name, apiVersion);
    if (autoscaler != null) {
      autoscaler.getSpec().setScaleTargetRef(
          new CrossVersionObjectReferenceBuilder().withKind("none").withName("none").build());
      createAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler);
    }
  }

  @Override
  public void enableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, String apiVersion) {
    HorizontalPodAutoscaler autoscaler = getAutoscaler(kubernetesConfig, encryptedDataDetails, name, apiVersion);
    if (autoscaler != null) {
      HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, name);
      if (controller != null) {
        autoscaler.getSpec().getScaleTargetRef().setKind(controller.getKind());
        autoscaler.getSpec().getScaleTargetRef().setName(name);
        createAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler);
      }
    }
  }

  @Override
  public void deleteAutoscaler(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).withName(name).delete();
  }

  @Override
  public List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String controllerName, int previousCount,
      int desiredCount, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog(String.format("Resize service [%s] in cluster [%s] from %s to %s instances",
                                              controllerName, clusterName, previousCount, desiredCount),
        LogLevel.INFO);

    if (previousCount != desiredCount) {
      HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName);
      if (controller instanceof ReplicationController) {
        rcOperations(kubernetesConfig, encryptedDataDetails).withName(controllerName).scale(desiredCount);
      } else if (controller instanceof Deployment) {
        deploymentOperations(kubernetesConfig, encryptedDataDetails).withName(controllerName).scale(desiredCount);
      } else if (controller instanceof ReplicaSet) {
        replicaOperations(kubernetesConfig, encryptedDataDetails).withName(controllerName).scale(desiredCount);
      } else if (controller instanceof StatefulSet) {
        statefulOperations(kubernetesConfig, encryptedDataDetails).withName(controllerName).scale(desiredCount);
      } else if (controller instanceof DaemonSet) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "DaemonSet runs one instance per cluster node and cannot be scaled.");
      }

      logger.info("Scaled controller {} in cluster {} from {} to {} instances", controllerName, clusterName,
          previousCount, desiredCount);
    }
    return getContainerInfosWhenReady(kubernetesConfig, encryptedDataDetails, controllerName, previousCount,
        desiredCount, serviceSteadyStateTimeout, executionLogCallback);
  }

  @Override
  public List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    List<Pod> pods = waitForPodsToBeRunning(kubernetesConfig, encryptedDataDetails, controllerName, previousCount,
        desiredCount, serviceSteadyStateTimeout, executionLogCallback);
    List<ContainerInfo> containerInfos = new ArrayList<>();
    boolean hasErrors = false;
    if (pods.size() != desiredCount) {
      hasErrors = true;
      String msg = String.format("Pod count did not reach desired count (%d/%d)", pods.size(), desiredCount);
      logger.error(msg);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    }
    for (Pod pod : pods) {
      String podName = pod.getMetadata().getName();
      String containerId = !pod.getStatus().getContainerStatuses().isEmpty()
          ? StringUtils.substring(pod.getStatus().getContainerStatuses().get(0).getContainerID(), 9, 21)
          : "";
      ContainerInfoBuilder containerInfoBuilder = ContainerInfo.builder().hostName(podName).containerId(containerId);
      Set<String> images = getControllerImages(getController(kubernetesConfig, encryptedDataDetails, controllerName));

      if (desiredCount > 0 && !podHasImages(pod, images)) {
        hasErrors = true;
        String msg = String.format("Pod %s does not have image %s", podName, images);
        logger.error(msg);
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      }

      if (desiredCount > 0 && desiredCount >= previousCount) {
        if (!isRunning(pod)) {
          hasErrors = true;
          String msg = String.format("Pod %s failed to start", podName);
          logger.error(msg);
          executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        }

        if (!inSteadyState(pod)) {
          hasErrors = true;
          String msg = String.format("Pod %s failed to reach steady state", podName);
          logger.error(msg);
          executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        }
      }

      if (!hasErrors) {
        containerInfoBuilder.status(Status.SUCCESS);
        logger.info("Pod {} started successfully", podName);
        executionLogCallback.saveExecutionLog(String.format("Pod [%s] is running. Host IP: %s. Pod IP: %s", podName,
                                                  pod.getStatus().getHostIP(), pod.getStatus().getPodIP()),
            LogLevel.INFO);
      } else {
        containerInfoBuilder.status(Status.FAILURE);
        String containerMessage = Joiner.on("], [").join(
            pod.getStatus().getContainerStatuses().stream().map(this ::getContainerStatusMessage).collect(toList()));
        String conditionMessage = Joiner.on("], [").join(
            pod.getStatus().getConditions().stream().map(this ::getPodConditionMessage).collect(toList()));
        String reason = Joiner.on("], [").join(pod.getStatus()
                                                   .getContainerStatuses()
                                                   .stream()
                                                   .map(containerStatus
                                                       -> containerStatus.getState().getTerminated() != null
                                                           ? containerStatus.getState().getTerminated().getReason()
                                                           : containerStatus.getState().getWaiting() != null
                                                               ? containerStatus.getState().getWaiting().getReason()
                                                               : RUNNING)
                                                   .collect(toList()));
        String msg = String.format(
            "Pod [%s] has state [%s]. Current status: phase - %s. Container status: [%s]. Condition: [%s].", podName,
            reason, pod.getStatus().getPhase(), containerMessage, conditionMessage);
        logger.error(msg);
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      }
      containerInfos.add(containerInfoBuilder.build());
    }
    return containerInfos;
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName);
    listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .filter(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).isPresent())
        .sorted(comparingInt(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).orElse(-1)))
        .forEach(ctrl -> result.put(ctrl.getMetadata().getName(), getControllerPodCount(ctrl)));
    return result;
  }

  private boolean inSteadyState(Pod pod) {
    List<PodCondition> conditions = pod.getStatus().getConditions();
    return isNotEmpty(conditions)
        && conditions.stream().allMatch(podCondition -> "True".equals(podCondition.getStatus()));
  }

  private boolean isRunning(Pod pod) {
    return pod.getStatus().getPhase().equals(RUNNING);
  }

  private boolean podHasImages(Pod pod, Set<String> images) {
    return pod.getSpec().getContainers().stream().map(Container::getImage).collect(toList()).containsAll(images);
  }

  private String getContainerStatusMessage(ContainerStatus status) {
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
  }

  private String getPodConditionMessage(PodCondition cond) {
    String msg = cond.getType() + ": " + cond.getStatus();
    if (cond.getReason() != null) {
      msg += " - " + cond.getReason();
    }
    if (cond.getMessage() != null) {
      msg += " - " + cond.getMessage();
    }
    return msg;
  }

  @Override
  public Optional<Integer> getControllerPodCount(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, name);
    if (controller != null) {
      Integer count = getControllerPodCount(controller);
      return count == null ? Optional.empty() : Optional.of(count);
    }
    return Optional.empty();
  }

  @Override
  public Integer getControllerPodCount(HasMetadata controller) {
    if (controller instanceof ReplicationController) {
      return ((ReplicationController) controller).getSpec().getReplicas();
    } else if (controller instanceof Deployment) {
      return ((Deployment) controller).getSpec().getReplicas();
    } else if (controller instanceof ReplicaSet) {
      return ((ReplicaSet) controller).getSpec().getReplicas();
    } else if (controller instanceof StatefulSet) {
      return ((StatefulSet) controller).getSpec().getReplicas();
    } else if (controller instanceof DaemonSet) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "DaemonSet runs one instance per cluster node and cannot be scaled.");
    }
    return null;
  }

  private PodTemplateSpec getPodTemplateSpec(HasMetadata controller) {
    PodTemplateSpec podTemplateSpec = null;
    if (controller instanceof ReplicationController) {
      podTemplateSpec = ((ReplicationController) controller).getSpec().getTemplate();
    } else if (controller instanceof Deployment) {
      podTemplateSpec = ((Deployment) controller).getSpec().getTemplate();
    } else if (controller instanceof DaemonSet) {
      podTemplateSpec = ((DaemonSet) controller).getSpec().getTemplate();
    } else if (controller instanceof ReplicaSet) {
      podTemplateSpec = ((ReplicaSet) controller).getSpec().getTemplate();
    } else if (controller instanceof StatefulSet) {
      podTemplateSpec = ((StatefulSet) controller).getSpec().getTemplate();
    }
    return podTemplateSpec;
  }

  private NonNamespaceOperation<ReplicationController, ReplicationControllerList, DoneableReplicationController,
      RollableScalableResource<ReplicationController, DoneableReplicationController>>
  rcOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .replicationControllers()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>>
  deploymentOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .deployments()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  private NonNamespaceOperation<ReplicaSet, ReplicaSetList, DoneableReplicaSet,
      RollableScalableResource<ReplicaSet, DoneableReplicaSet>>
  replicaOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .replicaSets()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  private NonNamespaceOperation<DaemonSet, DaemonSetList, DoneableDaemonSet, Resource<DaemonSet, DoneableDaemonSet>>
  daemonOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .daemonSets()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  private NonNamespaceOperation<StatefulSet, StatefulSetList, DoneableStatefulSet,
      RollableScalableResource<StatefulSet, DoneableStatefulSet>>
  statefulOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .apps()
        .statefulSets()
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
  public Ingress createOrReplaceIngress(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Ingress definition) {
    String name = definition.getMetadata().getName();
    Ingress ingress = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                          .extensions()
                          .ingresses()
                          .inNamespace(kubernetesConfig.getNamespace())
                          .withName(name)
                          .get();
    logger.info("{} ingress [{}]", ingress == null ? "Creating" : "Replacing", name);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .ingresses()
        .inNamespace(kubernetesConfig.getNamespace())
        .createOrReplace(definition);
  }

  @Override
  public Ingress getIngress(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    return isNotBlank(name) ? kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                                  .extensions()
                                  .ingresses()
                                  .inNamespace(kubernetesConfig.getNamespace())
                                  .withName(name)
                                  .get()
                            : null;
  }

  @Override
  public void deleteIngress(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    logger.info("Deleting service {}", name);
    kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .ingresses()
        .inNamespace(kubernetesConfig.getNamespace())
        .withName(name)
        .delete();
  }

  @Override
  public ConfigMap createOrReplaceConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, ConfigMap definition) {
    String name = definition.getMetadata().getName();
    ConfigMap configMap = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                              .configMaps()
                              .inNamespace(kubernetesConfig.getNamespace())
                              .withName(name)
                              .get();
    logger.info("{} config map [{}]", configMap == null ? "Creating" : "Replacing", name);
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .configMaps()
        .inNamespace(kubernetesConfig.getNamespace())
        .createOrReplace(definition);
  }

  @Override
  public void deleteConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .configMaps()
        .inNamespace(kubernetesConfig.getNamespace())
        .withName(name)
        .delete();
  }

  @Override
  public IstioResource createOrReplaceRouteRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, IstioResource definition) {
    String name = definition.getMetadata().getName();
    logger.info("Registering route rule [{}]", name);
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig, encryptedDataDetails);
    try {
      istioClient.unregisterCustomResource(definition);
    } catch (Exception e) {
      // Do nothing
    }
    return istioClient.registerCustomResource(definition);
  }

  @Override
  public void deleteRouteRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig, encryptedDataDetails);
    try {
      istioClient.unregisterCustomResource(new IstioResourceBuilder()
                                               .withNewMetadata()
                                               .withName(name)
                                               .withNamespace(kubernetesConfig.getNamespace())
                                               .endMetadata()
                                               .build());
    } catch (Exception e) {
      // Do nothing
    }
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
  public void deleteSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String secretName) {
    kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .secrets()
        .inNamespace(kubernetesConfig.getNamespace())
        .withName(secretName)
        .delete();
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

  @Override
  public NodeList getNodes(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails).nodes().list();
  }

  @Override
  public void waitForPodsToStop(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> labels, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    String waitingMsg = "Waiting for pods to stop...";
    logger.info(waitingMsg);
    try {
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          executionLogCallback.saveExecutionLog(waitingMsg, LogLevel.INFO);
          int size = kubernetesClient.pods()
                         .inNamespace(kubernetesConfig.getNamespace())
                         .withLabels(labels)
                         .list()
                         .getItems()
                         .size();
          if (size <= 0) {
            return true;
          }
          sleep(ofSeconds(5));
        }
      }, serviceSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for pods to stop";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e).addParam("message", "Error while waiting for pods to stop");
    }
  }

  private List<Pod> waitForPodsToBeRunning(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName);
    Set<String> images = getControllerImages(controller);
    Map<String, String> labels = getPodTemplateSpec(controller).getMetadata().getLabels();
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    logger.info("Waiting for pods to be ready...");
    AtomicBoolean countReached = new AtomicBoolean(false);
    AtomicBoolean haveImagesCountReached = new AtomicBoolean(false);
    AtomicBoolean runningCountReached = new AtomicBoolean(false);
    AtomicBoolean steadyStateCountReached = new AtomicBoolean(false);
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<Pod> pods =
              kubernetesClient.pods().inNamespace(kubernetesConfig.getNamespace()).withLabels(labels).list().getItems();

          pods.stream()
              .filter(pod -> "Failed".equals(pod.getStatus().getPhase()))
              .forEach(pod
                  -> kubernetesClient.pods()
                         .inNamespace(kubernetesConfig.getNamespace())
                         .withName(pod.getMetadata().getName())
                         .delete());

          if (pods.size() != desiredCount) {
            executionLogCallback.saveExecutionLog(
                String.format("Waiting for desired number of pods [%d/%d]", pods.size(), desiredCount), LogLevel.INFO);
            sleep(ofSeconds(5));
            continue;
          }
          if (!countReached.getAndSet(true)) {
            executionLogCallback.saveExecutionLog(
                String.format("Desired number of pods reached [%d/%d]", pods.size(), desiredCount), LogLevel.INFO);
          }

          if (desiredCount > 0) {
            int haveImages = (int) pods.stream().filter(pod -> podHasImages(pod, images)).count();
            if (haveImages != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  String.format(
                      "Waiting for pods to be updated with image %s [%d/%d]", images, haveImages, desiredCount),
                  LogLevel.INFO);
              sleep(ofSeconds(5));
              continue;
            }
            if (!haveImagesCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  String.format("Pods are updated with image %s [%d/%d]", images, haveImages, desiredCount),
                  LogLevel.INFO);
            }
          }

          if (desiredCount > 0 && desiredCount >= previousCount) {
            int running = (int) pods.stream().filter(this ::isRunning).count();
            if (running != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  String.format("Waiting for pods to be running [%d/%d]", running, desiredCount), LogLevel.INFO);
              sleep(ofSeconds(10));
              continue;
            }
            if (!runningCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  String.format("Pods are running [%d/%d]", running, desiredCount), LogLevel.INFO);
            }

            int steadyState = (int) pods.stream().filter(this ::inSteadyState).count();
            if (steadyState != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  String.format("Waiting for pods to reach steady state [%d/%d]", steadyState, desiredCount),
                  LogLevel.INFO);
              sleep(ofSeconds(15));
              continue;
            }
            if (!steadyStateCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  String.format("Pods have reached steady state [%d/%d]", steadyState, desiredCount), LogLevel.INFO);
            }
          }
          return pods;
        }
      }, serviceSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for pods to be ready";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e)
          .addParam("message", "Error while waiting for pods to be ready");
    }

    return kubernetesClient.pods().inNamespace(kubernetesConfig.getNamespace()).withLabels(labels).list().getItems();
  }

  private Set<String> getControllerImages(HasMetadata controller) {
    PodTemplateSpec template = null;
    if (controller instanceof ReplicationController) {
      template = ((ReplicationController) controller).getSpec().getTemplate();
    } else if (controller instanceof Deployment) {
      template = ((Deployment) controller).getSpec().getTemplate();
    } else if (controller instanceof ReplicaSet) {
      template = ((ReplicaSet) controller).getSpec().getTemplate();
    } else if (controller instanceof StatefulSet) {
      template = ((StatefulSet) controller).getSpec().getTemplate();
    } else if (controller instanceof DaemonSet) {
      template = ((DaemonSet) controller).getSpec().getTemplate();
    }

    return template != null
        ? template.getSpec().getContainers().stream().map(Container::getImage).collect(Collectors.toSet())
        : emptySet();
  }

  public void checkStatus(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String rcName, String serviceName) {
    KubernetesClient client = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    String masterUrl = client.getMasterUrl().toString();
    ReplicationController rc =
        client.replicationControllers().inNamespace(kubernetesConfig.getNamespace()).withName(rcName).get();
    if (rc != null) {
      String rcLink = masterUrl + rc.getMetadata().getSelfLink().substring(1);
      logger.info("Controller {}: {}", rcName, rcLink);
    } else {
      logger.info("Controller {} does not exist", rcName);
    }
    Service service = client.services().inNamespace(kubernetesConfig.getNamespace()).withName(serviceName).get();
    if (service != null) {
      String serviceLink = masterUrl + service.getMetadata().getSelfLink().substring(1);
      logger.info("Service %s: {}, link: {}", serviceName, serviceLink);
    } else {
      logger.info("Service {} does not exist", serviceName);
    }
  }
}
