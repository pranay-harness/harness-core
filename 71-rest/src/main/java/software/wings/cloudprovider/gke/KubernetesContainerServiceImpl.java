package software.wings.cloudprovider.gke;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeUtf8StringToFile;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static software.wings.beans.command.ContainerApiVersions.KUBERNETES_V1;
import static software.wings.beans.command.KubernetesSetupCommandUnit.HARNESS_KUBERNETES_REVISION_LABEL_KEY;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.delegatetasks.k8s.K8sTask.MANIFEST_FILES_DIR;
import static software.wings.utils.KubernetesConvention.DASH;
import static software.wings.utils.KubernetesConvention.ReleaseHistoryKeyName;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.KubernetesConvention.getServiceNameFromControllerName;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.DoneableReplicationController;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
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
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.kubectl.AuthCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.internal.IstioSpecRegistry;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationWeight;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableVirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpec;
import me.snowdrop.istio.client.IstioClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.ContainerInfoBuilder;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.utils.Misc;

import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by brett on 2/9/17
 */
@Singleton
@Slf4j
public class KubernetesContainerServiceImpl implements KubernetesContainerService {
  private static final String RUNNING = "Running";
  private static final String KUBECONFIG_FILENAME = "config";
  private static final String WORKING_DIR_BASE = "./repository/k8s/";

  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private KubernetesHelperService kubernetesHelperService = new KubernetesHelperService();
  @Inject private TimeLimiter timeLimiter;
  @Inject private Clock clock;
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

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
      controller = rcOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                       .createOrReplace((ReplicationController) definition);
    } else if (definition instanceof Deployment) {
      controller = deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                       .createOrReplace((Deployment) definition);
    } else if (definition instanceof ReplicaSet) {
      controller = replicaOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                       .createOrReplace((ReplicaSet) definition);
    } else if (definition instanceof StatefulSet) {
      HasMetadata existing = getController(kubernetesConfig, encryptedDataDetails, name);
      if (existing != null && existing.getKind().equals("StatefulSet")) {
        controller = statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                         .withName(name)
                         .patch((StatefulSet) definition);
      } else {
        controller = statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                         .create((StatefulSet) definition);
      }
    } else if (definition instanceof DaemonSet) {
      controller = daemonOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                       .createOrReplace((DaemonSet) definition);
    }
    return controller;
  }

  @Override
  public HasMetadata getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    return getController(kubernetesConfig, encryptedDataDetails, name, kubernetesConfig.getNamespace());
  }

  @Override
  public HasMetadata getController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, String namespace) {
    try {
      return timeLimiter.callWithTimeout(
          getControllerInternal(kubernetesConfig, encryptedDataDetails, name, namespace), 2, TimeUnit.MINUTES, true);
    } catch (WingsException e) {
      throw e;
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e).addParam("message", "Timed out while getting controller");
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e).addParam("message", "Error while getting controller");
    }
  }

  private Callable<HasMetadata> getControllerInternal(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String name, String namespace) {
    return () -> {
      HasMetadata controller = null;
      logger.info("Trying to get controller for name {}", name);
      if (isNotBlank(name)) {
        boolean success = false;
        boolean allFailed = true;
        while (!success) {
          try {
            try {
              controller = rcOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
              allFailed = false;
            } catch (Exception e) {
              // Ignore
            }
            if (controller == null) {
              try {
                controller =
                    deploymentOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
                allFailed = false;
              } catch (Exception e) {
                // Ignore
              }
            }
            if (controller == null) {
              try {
                controller = replicaOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
                allFailed = false;
              } catch (Exception e) {
                // Ignore
              }
            }
            if (controller == null) {
              try {
                controller = statefulOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
                allFailed = false;
              } catch (Exception e) {
                // Ignore
              }
            }
            if (controller == null) {
              try {
                controller = daemonOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
                allFailed = false;
              } catch (Exception e) {
                // Ignore
              }
            }
            if (allFailed) {
              controller = deploymentOperations(kubernetesConfig, encryptedDataDetails, namespace).withName(name).get();
            } else {
              success = true;
            }
          } catch (Exception e) {
            logger.warn("Exception while getting controller {}: {}:{}", name, e.getClass().getSimpleName(),
                ExceptionUtils.getMessage(e));
            if (e.getCause() != null) {
              logger.warn("Caused by: {}:{}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }

            // Special handling of k8s client 401/403 error. No need to retry...
            if (e instanceof KubernetesClientException) {
              KubernetesClientException clientException = (KubernetesClientException) e;
              int code = clientException.getCode();
              // error code 0 means connectivity issue. It will retry.
              switch (code) {
                case SC_UNAUTHORIZED:
                  throw new WingsException(INVALID_CREDENTIAL, USER, e);
                case SC_FORBIDDEN:
                  throw new WingsException(ACCESS_DENIED, USER, e);
                default:
                  logger.warn("Got KubernetesClientException with error code {}", code);
                  break;
              }
            }

            sleep(ofSeconds(1));
            logger.info("Retrying getController {} ...", name);
          }
        }
      }
      logger.info("Got controller for name {}", name);
      return controller;
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<? extends HasMetadata> getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = new ArrayList<>();
    boolean allFailed = true;
    try {
      controllers.addAll((List) rcOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                             .withLabels(labels)
                             .list()
                             .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .withLabels(labels)
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) replicaOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .withLabels(labels)
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .withLabels(labels)
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) daemonOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .withLabels(labels)
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    if (allFailed) {
      controllers.addAll(
          (List) deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .withLabels(labels)
              .list()
              .getItems());
    }
    return controllers;
  }

  @Override
  public void validate(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptionDetails, boolean cloudCostEnabled) {
    listControllers(kubernetesConfig, encryptionDetails);

    if (cloudCostEnabled) {
      validateCEPermissions(kubernetesConfig);
    }
  }

  private void validateCEPermissions(KubernetesConfig kubernetesConfig) {
    String workingDirectory = null;
    Kubectl kubectl = null;

    try {
      workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                             .normalize()
                             .toAbsolutePath()
                             .toString();
      logger.info("Working directory: {}", workingDirectory);

      String kubeconfigFileContent = containerDeploymentDelegateHelper.getConfigFileContent(kubernetesConfig);

      createDirectoryIfDoesNotExist(workingDirectory);
      waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
      writeUtf8StringToFile(Paths.get(workingDirectory, KUBECONFIG_FILENAME).toString(), kubeconfigFileContent);

      createDirectoryIfDoesNotExist(Paths.get(workingDirectory, MANIFEST_FILES_DIR).toString());

      String kubectlPath = k8sGlobalConfigService.getKubectlPath();
      String configPath = KUBECONFIG_FILENAME;

      kubectl = Kubectl.client(kubectlPath, configPath);
      logger.info("kubectl path: {}", kubectlPath);

    } catch (Exception ex) {
      logger.error("Exception", ex);
    }

    if (workingDirectory == null || kubectl == null) {
      throw new InvalidRequestException("Failed to initialize kubectl");
    }

    List<String> verbs = new ArrayList<>(Arrays.asList("watch"));
    List<String> resources = new ArrayList<>(Arrays.asList("nodes", "pods"));

    validateAuth(kubectl, workingDirectory, resources, verbs);
  }

  private void validateAuth(Kubectl kubectl, String workingDirectory, List<String> resources, List<String> verbs) {
    int m = resources.size();
    int n = verbs.size();
    boolean[][] permissions = new boolean[m][n];

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        try {
          AuthCommand authCommand = kubectl.auth().verb(verbs.get(j)).resources(resources.get(i)).allNamespaces(true);
          ProcessResult authResult = K8sTaskHelper.executeCommandSilent(authCommand, workingDirectory);
          if (authResult != null
              && (authResult.getExitValue() == 0 && "yes".equals(authResult.getOutput().getUTF8().trim()))) {
            permissions[i][j] = true;
          }
        } catch (Exception ex) {
          logger.error("Exception", ex);
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < m; i++) {
      List<String> unauthorizedVerbs = new ArrayList<>();
      for (int j = 0; j < n; j++) {
        if (!permissions[i][j]) {
          unauthorizedVerbs.add(verbs.get(j));
        }
      }
      if (unauthorizedVerbs.size() > 0) {
        sb.append(
            String.format("[%s] %s in all namespaces%n", StringUtils.join(unauthorizedVerbs, ","), resources.get(i)));
      }
    }

    if (sb.length() != 0) {
      throw new InvalidRequestException("The provided serviceaccount is missing the following permissions.\n"
          + sb.toString() + "Please grant these required permissions to the service account.");
    }
  }

  @Override
  public void validate(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptionDetails) {
    validate(kubernetesConfig, encryptionDetails, false);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<? extends HasMetadata> listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    List<? extends HasMetadata> controllers = new ArrayList<>();
    boolean allFailed = true;
    try {
      controllers.addAll((List) rcOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
                             .list()
                             .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) replicaOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    try {
      controllers.addAll(
          (List) daemonOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .list()
              .getItems());
      allFailed = false;
    } catch (RuntimeException e) {
      // Ignore
    }
    if (allFailed) {
      controllers.addAll(
          (List) deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
              .list()
              .getItems());
    }
    return controllers;
  }

  @Override
  public void deleteController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    logger.info("Deleting controller {}", name);
    if (isNotBlank(name)) {
      HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, name);
      if (controller instanceof ReplicationController) {
        rcOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace()).withName(name).delete();
      } else if (controller instanceof Deployment) {
        deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(name)
            .delete();
      } else if (controller instanceof ReplicaSet) {
        replicaOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(name)
            .delete();
      } else if (controller instanceof StatefulSet) {
        statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(name)
            .delete();
      } else if (controller instanceof DaemonSet) {
        daemonOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(name)
            .delete();
      }
    }
  }

  @Override
  public HorizontalPodAutoscaler createOrReplaceAutoscaler(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String autoscalerYaml) {
    if (isNotBlank(autoscalerYaml)) {
      HorizontalPodAutoscaler hpa;
      try {
        hpa = KubernetesHelper.loadYaml(autoscalerYaml);
        hpa.getMetadata().setResourceVersion(null);
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse horizontal pod autoscaler YAML: " + autoscalerYaml);
      }
      String api = kubernetesHelperService.trimVersion(hpa.getApiVersion());

      if (KUBERNETES_V1.getVersionName().equals(api)) {
        return kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).createOrReplace(hpa);
      } else {
        return kubernetesHelperService.hpaOperationsForCustomMetricHPA(kubernetesConfig, encryptedDataDetails, api)
            .createOrReplace(hpa);
      }
    }
    return null;
  }

  @Override
  public HorizontalPodAutoscaler getAutoscaler(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String name, String apiVersion) {
    if (KUBERNETES_V1.getVersionName().equals(apiVersion) || isEmpty(apiVersion)) {
      return kubernetesHelperService.hpaOperations(kubernetesConfig, encryptedDataDetails).withName(name).get();
    } else {
      return kubernetesHelperService.hpaOperationsForCustomMetricHPA(kubernetesConfig, encryptedDataDetails, apiVersion)
          .withName(name)
          .get();
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

      if (controller == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Could not find a controller named " + controllerName);
      }
      if (controller instanceof ReplicationController) {
        rcOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(controllerName)
            .scale(desiredCount);
      } else if (controller instanceof Deployment) {
        deploymentOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(controllerName)
            .scale(desiredCount);
      } else if (controller instanceof ReplicaSet) {
        replicaOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(controllerName)
            .scale(desiredCount);
      } else if (controller instanceof StatefulSet) {
        statefulOperations(kubernetesConfig, encryptedDataDetails, kubernetesConfig.getNamespace())
            .withName(controllerName)
            .scale(desiredCount);
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
        desiredCount, serviceSteadyStateTimeout, originalPods, false, executionLogCallback, sizeChanged, startTime,
        kubernetesConfig.getNamespace());
  }

  @Override
  public List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, List<Pod> originalPods, boolean isNotVersioned,
      ExecutionLogCallback executionLogCallback, boolean wait, long startTime, String namespace) {
    List<Pod> pods = wait
        ? waitForPodsToBeRunning(kubernetesConfig, encryptedDataDetails, controllerName, previousCount, desiredCount,
              serviceSteadyStateTimeout, originalPods, isNotVersioned, startTime, namespace, executionLogCallback)
        : originalPods;
    int controllerDesiredCount =
        getControllerPodCount(getController(kubernetesConfig, encryptedDataDetails, controllerName, namespace));

    if (desiredCount == -1) {
      // This indicates wait for all pods to be in steady state. In case of HPA you won't know absolute numbers
      desiredCount = controllerDesiredCount;
    }

    Set<String> originalPodNames = originalPods.stream().map(pod -> pod.getMetadata().getName()).collect(toSet());
    List<ContainerInfo> containerInfos = new ArrayList<>();
    boolean hasErrors = false;
    if (wait && (pods.size() != desiredCount || controllerDesiredCount != desiredCount)) {
      hasErrors = true;
      String msg = "";
      if (controllerDesiredCount != desiredCount) {
        msg = format("Controller replica count is set to %d instead of %d. ", controllerDesiredCount, desiredCount);
      }
      if (pods.size() != desiredCount) {
        msg += format("Pod count did not reach desired count (%d/%d)", pods.size(), desiredCount);
      }
      logger.error(msg);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    }
    for (Pod pod : pods) {
      String podName = pod.getMetadata().getName();
      String containerId = !pod.getStatus().getContainerStatuses().isEmpty()
          ? StringUtils.substring(pod.getStatus().getContainerStatuses().get(0).getContainerID(), 9, 21)
          : "";
      ContainerInfoBuilder containerInfoBuilder = ContainerInfo.builder()
                                                      .hostName(podName)
                                                      .ip(pod.getStatus().getPodIP())
                                                      .containerId(containerId)
                                                      .workloadName(controllerName)
                                                      .podName(podName)
                                                      .newContainer(!originalPodNames.contains(podName));

      HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName, namespace);
      PodTemplateSpec podTemplateSpec = null;
      if (null != controller) {
        podTemplateSpec = getPodTemplateSpec(controller);
      } else {
        logger.warn("podTemplateSpec is null.");
      }
      Set<String> images = emptySet();
      if (null != podTemplateSpec) {
        images = getControllerImages(podTemplateSpec);
      } else {
        logger.warn("Images is null.");
      }

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
  public LinkedHashMap<String, Integer> getActiveServiceCounts(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName);
    listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> controllerNamePrefix.equals(getPrefixFromControllerName(ctrl.getMetadata().getName())))
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .sorted(comparingInt(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).orElse(-1)))
        .forEach(ctrl -> result.put(ctrl.getMetadata().getName(), getControllerPodCount(ctrl)));
    return result;
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCountsWithLabels(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    getControllers(kubernetesConfig, encryptedDataDetails, labels)
        .stream()
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .sorted(comparingInt(
            ctrl -> Integer.parseInt(ctrl.getMetadata().getLabels().get(HARNESS_KUBERNETES_REVISION_LABEL_KEY))))
        .forEach(ctrl -> result.put(ctrl.getMetadata().getName(), getControllerPodCount(ctrl)));
    return result;
  }

  @Override
  public Map<String, String> getActiveServiceImages(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, String imagePrefix) {
    Map<String, String> result = new HashMap<>();
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName);
    listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> getControllerPodCount(ctrl) > 0)
        .filter(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).isPresent())
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
    } else {
      throw new InvalidRequestException(
          format("Unhandled kubernetes resource type [%s] for getting the pod count", controller.getKind()));
    }
  }

  @Override
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
  rcOperations(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String namespace) {
    namespace = isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .replicationControllers()
        .inNamespace(namespace);
  }

  private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment,
      ScalableResource<Deployment, DoneableDeployment>>
  deploymentOperations(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String namespace) {
    namespace = isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .deployments()
        .inNamespace(namespace);
  }

  private NonNamespaceOperation<ReplicaSet, ReplicaSetList, DoneableReplicaSet,
      RollableScalableResource<ReplicaSet, DoneableReplicaSet>>
  replicaOperations(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String namespace) {
    namespace = isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .replicaSets()
        .inNamespace(namespace);
  }

  private NonNamespaceOperation<DaemonSet, DaemonSetList, DoneableDaemonSet, Resource<DaemonSet, DoneableDaemonSet>>
  daemonOperations(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String namespace) {
    namespace = isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .extensions()
        .daemonSets()
        .inNamespace(namespace);
  }

  private NonNamespaceOperation<StatefulSet, StatefulSetList, DoneableStatefulSet,
      RollableScalableResource<StatefulSet, DoneableStatefulSet>>
  statefulOperations(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String namespace) {
    namespace = isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace();
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .apps()
        .statefulSets()
        .inNamespace(namespace);
  }

  @Override
  public Service createOrReplaceService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Service definition) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .services()
        .inNamespace(kubernetesConfig.getNamespace())
        .createOrReplace(definition);
  }

  @Override
  public Service getService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, String namespace) {
    return isNotBlank(name) ? kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                                  .services()
                                  .inNamespace(isNotBlank(namespace) ? namespace : kubernetesConfig.getNamespace())
                                  .withName(name)
                                  .get()
                            : null;
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
  public IstioResource createOrReplaceIstioResource(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, IstioResource definition) {
    String name = definition.getMetadata().getName();
    String kind = definition.getKind();
    logger.info("Registering {} [{}]", kind, name);
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig, encryptedDataDetails);
    return istioClient.registerOrUpdateCustomResource(definition);
  }

  @Override
  public VirtualService getIstioVirtualService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    try {
      VirtualService virtualService = new VirtualServiceBuilder().build();

      return kubernetesClient
          .customResources(getCustomResourceDefinition(kubernetesClient, virtualService), VirtualService.class,
              KubernetesResourceList.class, DoneableVirtualService.class)
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public DestinationRule getIstioDestinationRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    KubernetesClient kubernetesClient =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails);
    try {
      DestinationRule destinationRule = new DestinationRuleBuilder().build();

      return kubernetesClient
          .customResources(getCustomResourceDefinition(kubernetesClient, destinationRule), DestinationRule.class,
              KubernetesResourceList.class, DoneableDestinationRule.class)
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public CustomResourceDefinition getCustomResourceDefinition(KubernetesClient client, IstioResource resource) {
    final Optional<String> crdName = IstioSpecRegistry.getCRDNameFor(resource.getKind().toLowerCase());
    final CustomResourceDefinition customResourceDefinition =
        client.customResourceDefinitions().withName(crdName.get()).get();
    if (customResourceDefinition == null) {
      throw new IllegalArgumentException(
          format("Custom Resource Definition %s is not found in cluster %s", crdName, client.getMasterUrl()));
    }
    return customResourceDefinition;
  }

  @Override
  public void deleteIstioDestinationRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig, encryptedDataDetails);
    try {
      istioClient.unregisterCustomResource(new DestinationRuleBuilder()
                                               .withNewMetadata()
                                               .withName(name)
                                               .withNamespace(kubernetesConfig.getNamespace())
                                               .endMetadata()
                                               .build());
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
  }

  @Override
  public void deleteIstioVirtualService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name) {
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig, encryptedDataDetails);
    try {
      istioClient.unregisterCustomResource(new VirtualServiceBuilder()
                                               .withNewMetadata()
                                               .withName(name)
                                               .withNamespace(kubernetesConfig.getNamespace())
                                               .endMetadata()
                                               .build());
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
  }

  @Override
  public int getTrafficPercent(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerName) {
    String serviceName = getServiceNameFromControllerName(controllerName);
    IstioResource virtualService = getIstioVirtualService(kubernetesConfig, encryptedDataDetails, serviceName);
    Optional<Integer> revision = getRevisionFromControllerName(controllerName);
    if (virtualService == null || !revision.isPresent()) {
      return 0;
    }
    VirtualServiceSpec virtualServiceSpec = ((VirtualService) virtualService).getSpec();
    if (isEmpty(virtualServiceSpec.getHttp()) || isEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      return 0;
    }

    return virtualServiceSpec.getHttp()
        .get(0)
        .getRoute()
        .stream()
        .filter(dw -> Integer.toString(revision.get()).equals(dw.getDestination().getSubset()))
        .map(DestinationWeight::getWeight)
        .findFirst()
        .orElse(0);
  }

  @Override
  public Map<String, Integer> getTrafficWeights(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerName) {
    String serviceName = getServiceNameFromControllerName(controllerName);
    String controllerNamePrefix = getPrefixFromControllerName(controllerName);
    IstioResource virtualService = getIstioVirtualService(kubernetesConfig, encryptedDataDetails, serviceName);
    if (virtualService == null) {
      return new HashMap<>();
    }

    VirtualServiceSpec virtualServiceSpec = ((VirtualService) virtualService).getSpec();
    if (isEmpty(virtualServiceSpec.getHttp()) || isEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      return new HashMap<>();
    }
    List<DestinationWeight> destinationWeights = virtualServiceSpec.getHttp().get(0).getRoute();
    return destinationWeights.stream().collect(
        toMap(dw -> controllerNamePrefix + DASH + dw.getDestination().getSubset(), DestinationWeight::getWeight));
  }

  @Override
  public void createNamespaceIfNotExist(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      Namespace namespace = kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
                                .namespaces()
                                .withName(kubernetesConfig.getNamespace())
                                .get();
      if (namespace == null) {
        logger.info("Creating namespace [{}]", kubernetesConfig.getNamespace());
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
            .namespaces()
            .create(new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(kubernetesConfig.getNamespace())
                        .endMetadata()
                        .build());
      }
    } catch (Exception e) {
      logger.error("Couldn't get or create namespace {}", kubernetesConfig.getNamespace(), e);
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

  private List<Pod> prunePodsInFinalState(List<Pod> pods) {
    return pods.stream()
        .filter(pod
            -> !StringUtils.equals(pod.getStatus().getPhase(), "Failed")
                && !StringUtils.equals(pod.getStatus().getPhase(), "Succeeded"))
        .collect(toList());
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

          showPodEvents(
              kubernetesClient, namespace, pods, originalPodNames, seenEvents, startTime, executionLogCallback);

          pods = prunePodsInFinalState(pods);
          if (pods.size() <= 0) {
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
      throw new WingsException(ErrorCode.GENERAL_ERROR, e).addParam("message", "Error while waiting for pods to stop");
    }
  }

  private List<Pod> waitForPodsToBeRunning(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, List<Pod> originalPods, boolean isNotVersioned, long startTime, String namespace,
      ExecutionLogCallback executionLogCallback) {
    HasMetadata controller = getController(kubernetesConfig, encryptedDataDetails, controllerName, namespace);
    if (controller == null) {
      throw new InvalidArgumentsException(Pair.of(controllerName, "is null"));
    }
    PodTemplateSpec podTemplateSpec = getPodTemplateSpec(controller);
    if (podTemplateSpec == null) {
      throw new InvalidArgumentsException(Pair.of(controllerName + " pod spec", "is null"));
    }
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

    try {
      int waitMinutes = serviceSteadyStateTimeout > 0 ? serviceSteadyStateTimeout : DEFAULT_STEADY_STATE_TIMEOUT;
      return timeLimiter.callWithTimeout(() -> {
        Set<String> seenEvents = new HashSet<>();

        while (true) {
          try {
            int absoluteDesiredCount = desiredCount;
            HasMetadata currentController =
                getController(kubernetesConfig, encryptedDataDetails, controllerName, namespace);
            if (currentController != null) {
              int controllerDesiredCount = getControllerPodCount(currentController);
              absoluteDesiredCount = (desiredCount == -1) ? controllerDesiredCount : desiredCount;
              if (controllerDesiredCount != absoluteDesiredCount) {
                String msg = format("Replica count is set to %d instead of %d. [Could be due to HPA.]",
                    controllerDesiredCount, absoluteDesiredCount);
                logger.warn(msg);
                executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
              }
            } else {
              String msg = "Couldn't find controller " + controllerName;
              logger.error(msg);
              executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
            }

            showControllerEvents(
                kubernetesClient, namespace, controllerName, seenEvents, startTime, executionLogCallback);

            List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();

            // Show pod events
            showPodEvents(
                kubernetesClient, namespace, pods, originalPodNames, seenEvents, startTime, executionLogCallback);

            pods = prunePodsInFinalState(pods);

            // Check current state
            if (pods.size() != absoluteDesiredCount) {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for desired number of pods [%d/%d]", pods.size(), absoluteDesiredCount));
              sleep(ofSeconds(5));
              continue;
            }
            if (!countReached.getAndSet(true)) {
              executionLogCallback.saveExecutionLog(
                  format("Desired number of pods reached [%d/%d]", pods.size(), absoluteDesiredCount));
            }

            if (absoluteDesiredCount > 0) {
              int haveImages = (int) pods.stream().filter(pod -> podHasImages(pod, images)).count();
              if (haveImages != absoluteDesiredCount) {
                executionLogCallback.saveExecutionLog(format("Waiting for pods to be updated with image %s [%d/%d]",
                                                          images, haveImages, absoluteDesiredCount),
                    LogLevel.INFO);
                sleep(ofSeconds(5));
                continue;
              }
              if (!haveImagesCountReached.getAndSet(true)) {
                executionLogCallback.saveExecutionLog(
                    format("Pods are updated with image %s [%d/%d]", images, haveImages, absoluteDesiredCount));
              }
            }

            if (isNotVersioned || absoluteDesiredCount > previousCount) {
              int running = (int) pods.stream().filter(this ::isRunning).count();
              if (running != absoluteDesiredCount) {
                executionLogCallback.saveExecutionLog(
                    format("Waiting for pods to be running [%d/%d]", running, absoluteDesiredCount));
                sleep(ofSeconds(10));
                continue;
              }
              if (!runningCountReached.getAndSet(true)) {
                executionLogCallback.saveExecutionLog(
                    format("Pods are running [%d/%d]", running, absoluteDesiredCount));
              }

              int steadyState = (int) pods.stream().filter(this ::inSteadyState).count();
              if (steadyState != absoluteDesiredCount) {
                executionLogCallback.saveExecutionLog(
                    format("Waiting for pods to reach steady state [%d/%d]", steadyState, absoluteDesiredCount));
                sleep(ofSeconds(15));
                continue;
              }
              if (!steadyStateCountReached.getAndSet(true)) {
                executionLogCallback.saveExecutionLog(
                    format("Pods have reached steady state [%d/%d]", steadyState, absoluteDesiredCount));
              }
            }
            return pods;
          } catch (Exception e) {
            logger.error("Exception in pod state wait loop.", e);
            executionLogCallback.saveExecutionLog("Error while waiting for pods to be ready", LogLevel.ERROR);
            Misc.logAllMessages(e, executionLogCallback);
            executionLogCallback.saveExecutionLog("Continuing to wait...", LogLevel.ERROR);
            sleep(ofSeconds(15));
          }
        }
      }, waitMinutes, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for pods to be ready";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e)
          .addParam("message", "Error while waiting for pods to be ready");
    }

    return kubernetesClient.pods().inNamespace(namespace).withLabels(labels).list().getItems();
  }

  private void showPodEvents(KubernetesClient kubernetesClient, String namespace, List<Pod> currentPods,
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
        executionLogCallback.saveExecutionLog("\n****  Kubernetes Pod Events  ****");
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
      logger.error("Failed to process kubernetes pod events", e);
    }
  }

  private void showControllerEvents(KubernetesClient kubernetesClient, String namespace, String controllerName,
      Set<String> seenEvents, long startTime, ExecutionLogCallback executionLogCallback) {
    try {
      List<Event> newEvents = kubernetesClient.events()
                                  .inNamespace(namespace)
                                  .list()
                                  .getItems()
                                  .stream()
                                  .filter(evt -> !seenEvents.contains(evt.getMetadata().getName()))
                                  .filter(evt -> controllerName.equals(evt.getInvolvedObject().getName()))
                                  .filter(evt -> DateTime.parse(evt.getLastTimestamp()).getMillis() > startTime)
                                  .collect(toList());

      if (isNotEmpty(newEvents)) {
        executionLogCallback.saveExecutionLog("\n****  Kubernetes Controller Events  ****");
        executionLogCallback.saveExecutionLog("  Controller: " + controllerName);
        newEvents.forEach(evt -> executionLogCallback.saveExecutionLog("   - " + evt.getMessage()));
        executionLogCallback.saveExecutionLog("");
        seenEvents.addAll(newEvents.stream().map(evt -> evt.getMetadata().getName()).collect(toList()));
      }
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
      logger.error("Failed to process kubernetes controller events", e);
    }
  }

  @Override
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
      logger.info("Service: {}, link: {}", serviceName, serviceLink);
    } else {
      logger.info("Service {} does not exist", serviceName);
    }
  }

  @Override
  public String fetchReleaseHistory(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String releaseName) {
    String releaseHistory = "";
    ConfigMap configMap = getConfigMap(kubernetesConfig, encryptedDataDetails, releaseName);
    if (configMap != null && configMap.getData() != null && configMap.getData().containsKey(ReleaseHistoryKeyName)) {
      releaseHistory = configMap.getData().get(ReleaseHistoryKeyName);
    }

    return releaseHistory;
  }

  @Override
  public void saveReleaseHistory(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String releaseName, String releaseHistory) {
    try {
      ConfigMap configMap = getConfigMap(kubernetesConfig, encryptedDataDetails, releaseName);
      if (configMap == null) {
        configMap = new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName(releaseName)
                        .withNamespace(kubernetesConfig.getNamespace())
                        .endMetadata()
                        .withData(ImmutableMap.of(ReleaseHistoryKeyName, releaseHistory))
                        .build();
      } else {
        Map data = configMap.getData();
        data.put(ReleaseHistoryKeyName, releaseHistory);
        configMap.setData(data);
      }
      createOrReplaceConfigMap(kubernetesConfig, encryptedDataDetails, configMap);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e)
          .addParam("message", "Failed to save release History. " + ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<Pod> getRunningPodsWithLabels(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String namespace, Map<String, String> labels) {
    return kubernetesHelperService.getKubernetesClient(kubernetesConfig, encryptedDataDetails)
        .pods()
        .inNamespace(namespace)
        .withLabels(labels)
        .list()
        .getItems()
        .stream()
        .filter(pod
            -> StringUtils.isBlank(pod.getMetadata().getDeletionTimestamp())
                && StringUtils.equals(pod.getStatus().getPhase(), "Running"))
        .collect(Collectors.toList());
  }
}
