package software.wings.cloudprovider.gke;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.KubernetesConvention.getServiceNameFromControllerName;

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
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
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
import me.snowdrop.istio.api.internal.IstioSpecRegistry;
import me.snowdrop.istio.api.model.DoneableIstioResource;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.api.model.v1.routing.DestinationWeight;
import me.snowdrop.istio.api.model.v1.routing.RouteRule;
import me.snowdrop.istio.client.IstioClient;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
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
import software.wings.utils.Misc;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by brett on 2/9/17
 */
@Singleton
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesContainerServiceImpl.class);

  private static final String RUNNING = "Running";

  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;

  @Override
  public List<Namespace> listNamespaces(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .namespaces()
        .list()
        .getItems();
  }

  @Override
  public HasMetadata createOrReplaceController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, HasMetadata definition) {
    String name = definition.getMetadata().getName();
    logger.info("Creating {} {}", definition.getKind(), name);

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
      HasMetadata existing = getController(kubernetesConfig, encryptedDataDetails, name);
      if (existing != null && existing.getKind().equals("StatefulSet")) {
        statefulOperations(kubernetesConfig, encryptedDataDetails).withName(name).patch((StatefulSet) definition);
      } else {
        controller = statefulOperations(kubernetesConfig, encryptedDataDetails).create((StatefulSet) definition);
      }
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
  public HorizontalPodAutoscaler createOrReplaceAutoscaler(KubernetesConfig kubernetesConfig,
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
      createOrReplaceAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler);
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
        createOrReplaceAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler);
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
    boolean sizeChanged = previousCount != desiredCount;
    long startTime = clock.millis();
    List<Pod> originalPods = getRunningPods(kubernetesConfig, encryptedDataDetails, controllerName);
    if (sizeChanged) {
      executionLogCallback.saveExecutionLog(format("Resizing controller [%s] in cluster [%s] from %s to %s instances",
          controllerName, clusterName, previousCount, desiredCount));
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
    } else {
      executionLogCallback.saveExecutionLog(
          format("Controller [%s] in cluster [%s] stays at %s instances", controllerName, clusterName, previousCount));
    }
    return getContainerInfosWhenReady(kubernetesConfig, encryptedDataDetails, controllerName, previousCount,
        serviceSteadyStateTimeout, originalPods, false, executionLogCallback, sizeChanged, startTime);
  }

  @Override
  public List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount,
      int serviceSteadyStateTimeout, List<Pod> originalPods, boolean isNotVersioned,
      ExecutionLogCallback executionLogCallback, boolean wait, long startTime) {
    List<Pod> pods = wait
        ? waitForPodsToBeRunning(kubernetesConfig, encryptedDataDetails, controllerName, previousCount,
              serviceSteadyStateTimeout, originalPods, isNotVersioned, startTime, executionLogCallback)
        : originalPods;
    int desiredCount = getControllerPodCount(getController(kubernetesConfig, encryptedDataDetails, controllerName));
    List<ContainerInfo> containerInfos = new ArrayList<>();
    boolean hasErrors = false;
    if (pods.size() != desiredCount) {
      hasErrors = true;
      String msg = format("Pod count did not reach desired count (%d/%d)", pods.size(), desiredCount);
      logger.error(msg);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    }
    for (Pod pod : pods) {
      String podName = pod.getMetadata().getName();
      String containerId = !pod.getStatus().getContainerStatuses().isEmpty()
          ? StringUtils.substring(pod.getStatus().getContainerStatuses().get(0).getContainerID(), 9, 21)
          : "";
      ContainerInfoBuilder containerInfoBuilder = ContainerInfo.builder().hostName(podName).containerId(containerId);
      Set<String> images = getControllerImages(
          getPodTemplateSpec(getController(kubernetesConfig, encryptedDataDetails, controllerName)));

      if (desiredCount > 0 && !podHasImages(pod, images)) {
        hasErrors = true;
        String msg = format("Pod %s does not have image %s", podName, images);
        logger.error(msg);
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      }

      if (isNotVersioned || desiredCount > previousCount) {
        if (!isRunning(pod)) {
          hasErrors = true;
          String msg = format("Pod %s failed to start", podName);
          logger.error(msg);
          executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        }

        if (!inSteadyState(pod)) {
          hasErrors = true;
          String msg = format("Pod %s failed to reach steady state", podName);
          logger.error(msg);
          executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        }
      }

      if (!hasErrors) {
        containerInfoBuilder.status(Status.SUCCESS);
        logger.info("Pod {} started successfully", podName);
        executionLogCallback.saveExecutionLog(format("Pod [%s] is running. Host IP: %s. Pod IP: %s", podName,
            pod.getStatus().getHostIP(), pod.getStatus().getPodIP()));
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
        String msg =
            format("Pod [%s] has state [%s]. Current status: phase - %s. Container status: [%s]. Condition: [%s].",
                podName, reason, pod.getStatus().getPhase(), containerMessage, conditionMessage);
        logger.error(msg);
        executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
        executionLogCallback.saveExecutionLog("\nCheck Kubernetes console for more information");
      }
      containerInfos.add(containerInfoBuilder.build());
    }
    return containerInfos;
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, boolean isStatefulSet) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName, isStatefulSet);
    listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .filter(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName(), isStatefulSet).isPresent())
        .sorted(
            comparingInt(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName(), isStatefulSet).orElse(-1)))
        .forEach(ctrl -> result.put(ctrl.getMetadata().getName(), getControllerPodCount(ctrl)));
    return result;
  }

  @Override
  public Map<String, String> getActiveServiceImages(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, boolean isStatefulSet,
      String imagePrefix) {
    Map<String, String> result = new HashMap<>();
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName, isStatefulSet);
    listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .filter(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName(), isStatefulSet).isPresent())
        .forEach(ctrl
            -> result.put(ctrl.getMetadata().getName(),
                getPodTemplateSpec(ctrl)
                    .getSpec()
                    .getContainers()
                    .stream()
                    .map(Container::getImage)
                    .filter(image -> image.startsWith(imagePrefix + ":"))
                    .findFirst()
                    .orElse("none")));
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
      return ((DaemonSet) controller).getStatus().getDesiredNumberScheduled();
    }
    return null;
  }

  public PodTemplateSpec getPodTemplateSpec(HasMetadata controller) {
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
  public List<Service> getServices(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list()
        .getItems();
  }

  @Override
  public List<Service> listServices(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .list()
        .getItems();
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
  public ConfigMap getConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    try {
      return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
          .configMaps()
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      return null;
    }
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
  public IstioResource getRouteRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    try {
      return kubernetesClient
          .customResources(
              getCustomResourceDefinition(kubernetesClient, new IstioResourceBuilder().withKind("RouteRule").build()),
              IstioResource.class, KubernetesResourceList.class, DoneableIstioResource.class)
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      return null;
    }
  }

  private CustomResourceDefinition getCustomResourceDefinition(KubernetesClient client, IstioResource resource) {
    final String crdName = IstioSpecRegistry.getCRDNameFor(resource.getKind());
    final CustomResourceDefinition customResourceDefinition =
        client.customResourceDefinitions().withName(crdName).get();
    if (customResourceDefinition == null) {
      throw new IllegalArgumentException(
          format("Custom Resource Definition %s is not found in cluster %s", crdName, client.getMasterUrl()));
    }
    return customResourceDefinition;
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
  public int getTrafficPercent(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String controllerName, boolean isStatefulSet) {
    String serviceName = getServiceNameFromControllerName(controllerName, isStatefulSet);
    IstioResource routeRule = getRouteRule(kubernetesConfig, encryptedDataDetails, serviceName);
    Optional<Integer> revision = getRevisionFromControllerName(controllerName, isStatefulSet);
    if (routeRule == null || !revision.isPresent()) {
      return 0;
    }
    RouteRule routeRuleSpec = (RouteRule) routeRule.getSpec();
    return routeRuleSpec.getRoute()
        .stream()
        .filter(dw -> Integer.toString(revision.get()).equals(dw.getLabels().get(HARNESS_REVISION)))
        .map(DestinationWeight::getWeight)
        .findFirst()
        .orElse(0);
  }

  @Override
  public Map<String, Integer> getTrafficWeights(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, boolean isStatefulSet) {
    String serviceName = getServiceNameFromControllerName(controllerName, isStatefulSet);
    String controllerNamePrefix = getPrefixFromControllerName(controllerName, isStatefulSet);
    IstioResource routeRule = getRouteRule(kubernetesConfig, encryptedDataDetails, serviceName);
    if (routeRule == null) {
      return new HashMap<>();
    }
    RouteRule routeRuleSpec = (RouteRule) routeRule.getSpec();
    return routeRuleSpec.getRoute().stream().collect(
        toMap(dw -> controllerNamePrefix + dw.getLabels().get(HARNESS_REVISION), DestinationWeight::getWeight));
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
  public List<Pod> getPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .pods()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list()
        .getItems();
  }

  @Override
  public NodeList getNodes(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails).nodes().list();
  }

  @Override
  public void waitForPodsToStop(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> labels, int serviceSteadyStateTimeout, List<Pod> originalPods, long startTime,
      ExecutionLogCallback executionLogCallback) {
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    List<String> originalPodNames = originalPods.stream().map(pod -> pod.getMetadata().getName()).collect(toList());
    String namespace = kubernetesConfig.getNamespace();
    String waitingMsg = "Waiting for pods to stop...";
    logger.info(waitingMsg);
    try {
      timeLimiter.callWithTimeout(() -> {
        Set<String> seenEvents = new HashSet<>();

        while (true) {
          executionLogCallback.saveExecutionLog(waitingMsg);
          List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();
          int size = pods.size();
          showEvents(kubernetesClient, namespace, pods, originalPodNames, seenEvents, startTime, executionLogCallback);
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
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount,
      int serviceSteadyStateTimeout, List<Pod> originalPods, boolean isNotVersioned, long startTime,
      ExecutionLogCallback executionLogCallback) {
    HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName);
    PodTemplateSpec podTemplateSpec = getPodTemplateSpec(controller);
    Set<String> images = getControllerImages(podTemplateSpec);
    Map<String, String> labels = podTemplateSpec.getMetadata().getLabels();
    List<String> originalPodNames = originalPods.stream().map(pod -> pod.getMetadata().getName()).collect(toList());
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    logger.info("Waiting for pods to be ready...");
    AtomicBoolean countReached = new AtomicBoolean(false);
    AtomicBoolean haveImagesCountReached = new AtomicBoolean(false);
    AtomicBoolean runningCountReached = new AtomicBoolean(false);
    AtomicBoolean steadyStateCountReached = new AtomicBoolean(false);
    String namespace = kubernetesConfig.getNamespace();
    try {
      return timeLimiter.callWithTimeout(() -> {
        Set<String> seenEvents = new HashSet<>();

        while (true) {
          int desiredCount =
              getControllerPodCount(getController(kubernetesConfig, encryptedDataDetails, controllerName));
          List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();

          // Delete failed pods
          pods.stream()
              .filter(pod -> "Failed".equals(pod.getStatus().getPhase()))
              .forEach(
                  pod -> kubernetesClient.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).delete());

          // Show events
          showEvents(kubernetesClient, namespace, pods, originalPodNames, seenEvents, startTime, executionLogCallback);

          // Check current state
          if (pods.size() != desiredCount) {
            executionLogCallback.saveExecutionLog(
                format("Waiting for desired number of pods [%d/%d]", pods.size(), desiredCount));
            sleep(ofSeconds(5));
            continue;
          }
          if (!countReached.getAndSet(true)) {
            executionLogCallback.saveExecutionLog(
                format("Desired number of pods reached [%d/%d]", pods.size(), desiredCount));
          }

          if (desiredCount > 0) {
            int haveImages = (int) pods.stream().filter(pod -> podHasImages(pod, images)).count();
            if (haveImages != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for pods to be updated with image %s [%d/%d]", images, haveImages, desiredCount),
                  LogLevel.INFO);
              sleep(ofSeconds(5));
              continue;
            }
            if (!haveImagesCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  format("Pods are updated with image %s [%d/%d]", images, haveImages, desiredCount), LogLevel.INFO);
            }
          }

          if (isNotVersioned || desiredCount > previousCount) {
            int running = (int) pods.stream().filter(this ::isRunning).count();
            if (running != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for pods to be running [%d/%d]", running, desiredCount));
              sleep(ofSeconds(10));
              continue;
            }
            if (!runningCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(format("Pods are running [%d/%d]", running, desiredCount));
            }

            int steadyState = (int) pods.stream().filter(this ::inSteadyState).count();
            if (steadyState != desiredCount) {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for pods to reach steady state [%d/%d]", steadyState, desiredCount), LogLevel.INFO);
              sleep(ofSeconds(15));
              continue;
            }
            if (!steadyStateCountReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  format("Pods have reached steady state [%d/%d]", steadyState, desiredCount));
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

    return kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();
  }

  private void showEvents(KubernetesClient kubernetesClient, String namespace, List<Pod> currentPods,
      List<String> originalPodNames, Set<String> seenEvents, long startTime,
      ExecutionLogCallback executionLogCallback) {
    try {
      Set<String> podNames = new LinkedHashSet<>(originalPodNames);
      podNames.addAll(currentPods.stream().map(pod -> pod.getMetadata().getName()).collect(toList()));

      List<Event> newEvents = kubernetesClient.events()
                                  .inNamespace(namespace)
                                  .list()
                                  .getItems()
                                  .stream()
                                  .filter(evt -> !seenEvents.contains(evt.getMetadata().getName()))
                                  .filter(evt -> podNames.contains(evt.getInvolvedObject().getName()))
                                  .filter(evt -> DateTime.parse(evt.getLastTimestamp()).getMillis() > startTime)
                                  .collect(toList());

      if (isNotEmpty(newEvents)) {
        executionLogCallback.saveExecutionLog("\n****  Kubernetes Events  ****");
        podNames.forEach(podName -> {
          List<Event> podEvents =
              newEvents.stream().filter(evt -> evt.getInvolvedObject().getName().equals(podName)).collect(toList());
          if (isNotEmpty(podEvents)) {
            executionLogCallback.saveExecutionLog("  Pod: " + podName);
            podEvents.forEach(evt -> executionLogCallback.saveExecutionLog("   - " + evt.getMessage()));
          }
        });
        executionLogCallback.saveExecutionLog("");
        seenEvents.addAll(newEvents.stream().map(evt -> evt.getMetadata().getName()).collect(toList()));
      }
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
      logger.error("Failed to process kubernetes events", e);
    }
  }

  public List<Pod> getRunningPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerName) {
    HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName);
    PodTemplateSpec podTemplateSpec = getPodTemplateSpec(controller);
    if (podTemplateSpec == null) {
      return emptyList();
    }
    Map<String, String> labels = podTemplateSpec.getMetadata().getLabels();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .pods()
        .inNamespace(kubernetesConfig.getNamespace())
        .withLabels(labels)
        .list()
        .getItems();
  }

  private Set<String> getControllerImages(PodTemplateSpec template) {
    return template.getSpec().getContainers().stream().map(Container::getImage).collect(toSet());
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
