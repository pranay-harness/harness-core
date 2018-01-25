package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.KubernetesConvention.getKubernetesSecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesServiceName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerSpecBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetSpec;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.MapUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesSetupCommandUnit.class);

  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;
  @Inject @Transient private transient TimeLimiter timeLimiter;

  public KubernetesSetupCommandUnit() {
    super(CommandUnitType.KUBERNETES_SETUP);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected ContainerSetupCommandUnitExecutionData executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> edd, ContainerSetupParams containerSetupParams, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    KubernetesSetupParams setupParams = (KubernetesSetupParams) containerSetupParams;

    KubernetesConfig kubernetesConfig;
    List<EncryptedDataDetail> encryptedDataDetails;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
      encryptedDataDetails = edd;
    } else {
      kubernetesConfig = gkeClusterService.getCluster(
          cloudProviderSetting, edd, setupParams.getClusterName(), setupParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
      encryptedDataDetails = emptyList();
    }

    String lastCtrlName = lastController(kubernetesConfig, encryptedDataDetails, setupParams.getControllerNamePrefix());
    int revision = getRevisionFromControllerName(lastCtrlName).orElse(-1) + 1;

    String kubernetesServiceName = getKubernetesServiceName(setupParams.getControllerNamePrefix());

    if (setupParams.isRollback()) {
      executionLogCallback.saveExecutionLog("Rolling back setup", LogLevel.INFO);
      if (isNotBlank(setupParams.getPreviousDaemonSetYaml())) {
        performDaemonSetRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
      }
      if (isNotEmpty(setupParams.getActiveAutoscalers())) {
        performAutoscalerRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
      }
      executionLogCallback.saveExecutionLog("Rollback complete", LogLevel.INFO);
      return ContainerSetupCommandUnitExecutionData.builder()
          .containerServiceName(setupParams.getControllerNamePrefix())
          .build();
    }

    kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, encryptedDataDetails);

    String secretName = getKubernetesSecretName(setupParams.getImageDetails().getRegistryUrl());
    kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails,
        createRegistrySecret(
            secretName, kubernetesConfig.getNamespace(), setupParams.getImageDetails(), executionLogCallback));

    KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) setupParams.getContainerTask();
    if (kubernetesContainerTask == null) {
      kubernetesContainerTask = new KubernetesContainerTask();
      ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
      kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    boolean isDaemonSet = kubernetesContainerTask.kubernetesType() == DaemonSet.class;
    String containerServiceName = isDaemonSet
        ? setupParams.getControllerNamePrefix()
        : KubernetesConvention.getControllerName(setupParams.getControllerNamePrefix(), revision);

    Map<String, String> serviceLabels =
        ImmutableMap.<String, String>builder()
            .put("app", KubernetesConvention.getLabelValue(setupParams.getAppName()))
            .put("service", KubernetesConvention.getLabelValue(setupParams.getServiceName()))
            .put("env", KubernetesConvention.getLabelValue(setupParams.getEnvName()))
            .build();

    Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                               .putAll(serviceLabels)
                                               .put("revision", isDaemonSet ? "ds" : Integer.toString(revision))
                                               .build();

    HasMetadata controllerDefinition = createKubernetesControllerDefinition(kubernetesContainerTask,
        containerServiceName, controllerLabels, kubernetesConfig.getNamespace(), setupParams.getImageDetails(),
        secretName, serviceVariables, safeDisplayServiceVariables, executionLogCallback);
    kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails, controllerDefinition);

    String serviceClusterIP = null;
    String serviceLoadBalancerEndpoint = null;

    Service service =
        kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);

    if (setupParams.getServiceType() != null && setupParams.getServiceType() != KubernetesServiceType.None) {
      Service serviceDefinition =
          createServiceDefinition(kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels, setupParams);
      if (service != null) {
        // Keep the previous load balancer IP if it exists and a new one was not specified
        LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
        if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer
            && isEmpty(setupParams.getLoadBalancerIP()) && loadBalancer != null
            && !loadBalancer.getIngress().isEmpty()) {
          setupParams.setLoadBalancerIP(loadBalancer.getIngress().get(0).getIp());
          serviceDefinition = createServiceDefinition(
              kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels, setupParams);
        }
      }
      service =
          kubernetesContainerService.createOrReplaceService(kubernetesConfig, encryptedDataDetails, serviceDefinition);
      serviceClusterIP = service.getSpec().getClusterIP();

      if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer) {
        serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(
            kubernetesConfig, encryptedDataDetails, service, setupParams.getLoadBalancerIP(), executionLogCallback);
      }
    } else if (service != null) {
      executionLogCallback.saveExecutionLog(
          "Kubernetes service type set to 'None'. Deleting existing service " + kubernetesServiceName, LogLevel.INFO);
      kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
    }

    // Disable previous autoscalers
    if (isNotEmpty(setupParams.getActiveAutoscalers())) {
      setupParams.getActiveAutoscalers().forEach(autoscaler -> {
        executionLogCallback.saveExecutionLog("Disabling autoscaler " + autoscaler, LogLevel.INFO);
        kubernetesContainerService.disableAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler);
      });
    }

    // Create new autoscaler
    if (setupParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler autoscalerDefinition =
          createAutoscaler(containerServiceName, kubernetesConfig.getNamespace(), controllerLabels, setupParams);
      if (autoscalerDefinition != null) {
        executionLogCallback.saveExecutionLog("Creating autoscaler " + containerServiceName + " with min instances: "
                + setupParams.getMinAutoscaleInstances() + ", max instances: " + setupParams.getMaxAutoscaleInstances()
                + ", target CPU utilization: " + setupParams.getTargetCpuUtilizationPercentage() + "%",
            LogLevel.INFO);
        kubernetesContainerService.createAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerDefinition);
      }
    }

    String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();

    executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Controller Name: " + containerServiceName, LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Service Name: " + kubernetesServiceName, LogLevel.INFO);
    if (isNotBlank(serviceClusterIP)) {
      executionLogCallback.saveExecutionLog("Service Cluster IP: " + serviceClusterIP, LogLevel.INFO);
    }
    if (isNotBlank(serviceLoadBalancerEndpoint)) {
      executionLogCallback.saveExecutionLog("Load Balancer Endpoint: " + serviceLoadBalancerEndpoint, LogLevel.INFO);
    }
    executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);

    if (isDaemonSet) {
      listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
          executionLogCallback, kubernetesConfig, containerServiceName);
    } else {
      executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
      cleanup(kubernetesConfig, encryptedDataDetails, containerServiceName);
    }
    return ContainerSetupCommandUnitExecutionData.builder().containerServiceName(containerServiceName).build();
  }

  private void listContainerInfosWhenReady(List<EncryptedDataDetail> encryptedDataDetails,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback, KubernetesConfig kubernetesConfig,
      String containerServiceName) {
    int desiredCount = kubernetesContainerService.getNodes(kubernetesConfig, encryptedDataDetails).getItems().size();
    int previousCount =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName) != null
        ? desiredCount
        : 0;
    List<ContainerInfo> containerInfos =
        kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig, encryptedDataDetails,
            containerServiceName, previousCount, desiredCount, serviceSteadyStateTimeout, executionLogCallback);

    boolean allContainersSuccess =
        containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
    if (containerInfos.size() != desiredCount || !allContainersSuccess) {
      if (containerInfos.size() != desiredCount) {
        String message = String.format("Expected data for %d container%s but got %d", desiredCount,
            containerInfos.size() == 1 ? "" : "s", containerInfos.size());
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      if (!allContainersSuccess) {
        List<ContainerInfo> failed =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        String message = String.format("The following container%s did not have success status: %s",
            failed.size() == 1 ? "" : "s", failed.stream().map(ContainerInfo::getContainerId).collect(toList()));
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE)
          .addParam("message", "DaemonSet pods failed to reach desired count");
    }
    containerInfos.forEach(info
        -> executionLogCallback.saveExecutionLog("DaemonSet container ID: " + info.getContainerId(), LogLevel.INFO));
  }

  private void performDaemonSetRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    String daemonSetName = setupParams.getControllerNamePrefix();
    String daemonSetYaml = setupParams.getPreviousDaemonSetYaml();
    if (isNotBlank(daemonSetYaml)) {
      try {
        DaemonSet daemonSet = KubernetesHelper.loadYaml(daemonSetYaml);
        executionLogCallback.saveExecutionLog("Rolling back DaemonSet " + daemonSetName, LogLevel.INFO);
        kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails, daemonSet);
        executionLogCallback.saveExecutionLog("Rolled back to DaemonSet with image: "
                + daemonSet.getSpec()
                      .getTemplate()
                      .getSpec()
                      .getContainers()
                      .stream()
                      .map(Container::getImage)
                      .collect(toList()),
            LogLevel.INFO);
        listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, daemonSetName);
      } catch (IOException e) {
        executionLogCallback.saveExecutionLog("Error reading DaemonSet from yaml: " + daemonSetName, LogLevel.ERROR);
      }
    } else {
      executionLogCallback.saveExecutionLog(
          "DaemonSet " + daemonSetName + " did not exist previously. Deleting on rollback", LogLevel.INFO);
      HasMetadata daemonSet =
          kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      Map<String, String> labels = daemonSet.getMetadata().getLabels();
      kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      kubernetesContainerService.waitForPodsToStop(kubernetesConfig, encryptedDataDetails, labels,
          setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
    }
  }

  private void performAutoscalerRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    List<String> autoscalerNames = setupParams.getActiveAutoscalers();
    if (isNotEmpty(autoscalerNames)) {
      for (String autoscalerName : autoscalerNames) {
        executionLogCallback.saveExecutionLog("Enabling autoscaler " + autoscalerName, LogLevel.INFO);
        kubernetesContainerService.enableAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerName);
      }
    }
  }

  private Secret createRegistrySecret(
      String secretName, String namespace, ImageDetails imageDetails, ExecutionLogCallback executionLogCallback) {
    String credentialData = String.format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
    executionLogCallback.saveExecutionLog("Setting image pull secret " + secretName, LogLevel.INFO);
    return new SecretBuilder()
        .withData(ImmutableMap.of(".dockercfg", new String(Base64.getEncoder().encode(credentialData.getBytes()))))
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType("kubernetes.io/dockercfg")
        .withKind("Secret")
        .build();
  }

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Service service, String loadBalancerIP,
      ExecutionLogCallback executionLogCallback) {
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null
        && (loadBalancer.getIngress().isEmpty()
               || (isNotEmpty(loadBalancerIP) && !loadBalancerIP.equals(loadBalancer.getIngress().get(0).getIp())))) {
      executionLogCallback.saveExecutionLog(
          "Waiting for service " + serviceName + " load balancer to be ready", LogLevel.INFO);
      try {
        return timeLimiter.callWithTimeout(() -> {
          while (true) {
            LoadBalancerStatus loadBalancerStatus =
                kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, serviceName)
                    .getStatus()
                    .getLoadBalancer();
            if (!loadBalancerStatus.getIngress().isEmpty()
                && (isEmpty(loadBalancerIP) || loadBalancerIP.equals(loadBalancerStatus.getIngress().get(0).getIp()))) {
              LoadBalancerIngress loadBalancerIngress = loadBalancerStatus.getIngress().get(0);
              String loadBalancerEndpoint = isNotEmpty(loadBalancerIngress.getHostname())
                  ? loadBalancerIngress.getHostname()
                  : loadBalancerIngress.getIp();
              executionLogCallback.saveExecutionLog(
                  String.format(
                      "Service [%s] load balancer is ready with endpoint [%s]", serviceName, loadBalancerEndpoint),
                  LogLevel.INFO);
              return loadBalancerEndpoint;
            }
            sleep(ofSeconds(1));
          }
        }, 5L, TimeUnit.MINUTES, true);
      } catch (UncheckedTimeoutException e) {
        executionLogCallback.saveExecutionLog(
            String.format("Timed out waiting for service [%s] load balancer to be ready", serviceName), LogLevel.ERROR);
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }
    }
    return null;
  }

  private String lastController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerNamePrefix) {
    final HasMetadata[] lastReplicationController = {null};
    final AtomicInteger lastRevision = new AtomicInteger();
    kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix + KubernetesConvention.DOT))
        .forEach(ctrl -> {
          Optional<Integer> revision = getRevisionFromControllerName(ctrl.getMetadata().getName());
          if (revision.isPresent() && (lastReplicationController[0] == null || revision.get() > lastRevision.get())) {
            lastReplicationController[0] = ctrl;
            lastRevision.set(revision.get());
          }
        });
    return lastReplicationController[0] != null ? lastReplicationController[0].getMetadata().getName() : null;
  }

  private HasMetadata createKubernetesControllerDefinition(KubernetesContainerTask kubernetesContainerTask,
      String replicationControllerName, Map<String, String> controllerLabels, String namespace,
      ImageDetails imageDetails, String secretName, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    String containerName = KubernetesConvention.getContainerName(imageDetails.getName());
    String imageNameTag = imageDetails.getName() + ":" + imageDetails.getTag();

    HasMetadata kubernetesObj = kubernetesContainerTask.createController(containerName, imageNameTag, secretName);

    KubernetesHelper.setName(kubernetesObj, replicationControllerName);
    KubernetesHelper.setNamespace(kubernetesObj, namespace);
    KubernetesHelper.getOrCreateLabels(kubernetesObj).putAll(controllerLabels);

    configureTypeSpecificSpecs(
        controllerLabels, kubernetesObj, serviceVariables, safeDisplayServiceVariables, executionLogCallback);

    return kubernetesObj;
  }

  private void configureTypeSpecificSpecs(Map<String, String> controllerLabels, HasMetadata kubernetesObj,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ExecutionLogCallback executionLogCallback) {
    ObjectMeta objectMeta = null;
    PodSpec podSpec = null;
    if (kubernetesObj instanceof ReplicationController) {
      ReplicationControllerSpec rcSpec = ((ReplicationController) kubernetesObj).getSpec();
      rcSpec.setSelector(controllerLabels);
      rcSpec.setReplicas(0);
      objectMeta = rcSpec.getTemplate().getMetadata();
      podSpec = rcSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof Deployment) {
      DeploymentSpec depSpec = ((Deployment) kubernetesObj).getSpec();
      depSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      depSpec.setReplicas(0);
      objectMeta = depSpec.getTemplate().getMetadata();
      podSpec = depSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof DaemonSet) {
      DaemonSetSpec dsSpec = ((DaemonSet) kubernetesObj).getSpec();
      dsSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      objectMeta = dsSpec.getTemplate().getMetadata();
      podSpec = dsSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof ReplicaSet) {
      ReplicaSetSpec repSetSpec = ((ReplicaSet) kubernetesObj).getSpec();
      repSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      repSetSpec.setReplicas(0);
      objectMeta = repSetSpec.getTemplate().getMetadata();
      podSpec = repSetSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof StatefulSet) {
      StatefulSetSpec stateSetSpec = ((StatefulSet) kubernetesObj).getSpec();
      stateSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      stateSetSpec.setReplicas(0);
      objectMeta = stateSetSpec.getTemplate().getMetadata();
      podSpec = stateSetSpec.getTemplate().getSpec();
    }
    if (objectMeta != null) {
      Map<String, String> labels = objectMeta.getLabels();
      if (labels == null) {
        labels = new HashMap<>();
        objectMeta.setLabels(labels);
      }
      objectMeta.getLabels().putAll(controllerLabels);
    }

    if (podSpec != null) {
      // Set service variables as environment variables
      if (MapUtils.isNotEmpty(serviceVariables)) {
        if (MapUtils.isNotEmpty(safeDisplayServiceVariables)) {
          executionLogCallback.saveExecutionLog("Setting environment variables in container definition", LogLevel.INFO);
          for (String key : safeDisplayServiceVariables.keySet()) {
            executionLogCallback.saveExecutionLog(key + "=" + safeDisplayServiceVariables.get(key), LogLevel.INFO);
          }
        }
        Map<String, EnvVar> serviceEnvVars =
            serviceVariables.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> new EnvVarBuilder().withName(entry.getKey()).withValue(entry.getValue()).build()));
        for (Container container : podSpec.getContainers()) {
          Map<String, EnvVar> envVarsMap = new HashMap<>();
          if (container.getEnv() != null) {
            container.getEnv().forEach(envVar -> envVarsMap.put(envVar.getName(), envVar));
          }
          envVarsMap.putAll(serviceEnvVars);
          container.setEnv(new ArrayList<>(envVarsMap.values()));
        }
      }
    }
  }

  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(
      String serviceName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    ServiceSpecBuilder spec =
        new ServiceSpecBuilder().addToSelector(serviceLabels).withType(setupParams.getServiceType().name());

    if (setupParams.getServiceType() != KubernetesServiceType.ExternalName) {
      ServicePortBuilder servicePort = new ServicePortBuilder()
                                           .withProtocol(setupParams.getProtocol().name())
                                           .withPort(setupParams.getPort())
                                           .withNewTargetPort()
                                           .withIntVal(setupParams.getTargetPort())
                                           .endTargetPort();
      if (setupParams.getServiceType() == KubernetesServiceType.NodePort && setupParams.getNodePort() != null) {
        servicePort.withNodePort(setupParams.getNodePort());
      }
      spec.withPorts(ImmutableList.of(servicePort.build())); // TODO:: Allow more than one port

      if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer
          && isNotEmpty(setupParams.getLoadBalancerIP())) {
        spec.withLoadBalancerIP(setupParams.getLoadBalancerIP());
      }

      if (setupParams.getServiceType() == KubernetesServiceType.ClusterIP && isNotEmpty(setupParams.getClusterIP())) {
        spec.withClusterIP(setupParams.getClusterIP());
      }
    } else {
      spec.withExternalName(setupParams.getExternalName());
    }

    if (isNotEmpty(setupParams.getExternalIPs())) {
      spec.withExternalIPs(Arrays.stream(setupParams.getExternalIPs().split(",")).map(String::trim).collect(toList()));
    }

    return new ServiceBuilder()
        .withNewMetadata()
        .withName(serviceName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private HorizontalPodAutoscaler createAutoscaler(
      String autoscalerName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    HorizontalPodAutoscalerSpecBuilder spec =
        new HorizontalPodAutoscalerSpecBuilder()
            .withMinReplicas(setupParams.getMinAutoscaleInstances())
            .withMaxReplicas(setupParams.getMaxAutoscaleInstances())
            .withTargetCPUUtilizationPercentage(setupParams.getTargetCpuUtilizationPercentage())
            .withNewScaleTargetRef()
            .withKind("none")
            .withName("none")
            .endScaleTargetRef();
    return new HorizontalPodAutoscalerBuilder()
        .withNewMetadata()
        .withName(autoscalerName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private void cleanup(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    Optional<Integer> revision = getRevisionFromControllerName(containerServiceName);
    if (revision.isPresent() && revision.get() >= KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision.get() - KEEP_N_REVISIONS + 1;
      String controllerNamePrefix = KubernetesConvention.getPrefixFromControllerName(containerServiceName);
      kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
          .stream()
          .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(ctrl -> kubernetesContainerService.getControllerPodCount(ctrl) == 0)
          .forEach(ctrl -> {
            String controllerName = ctrl.getMetadata().getName();
            Optional<Integer> ctrlRevision = getRevisionFromControllerName(controllerName);
            if (ctrlRevision.isPresent() && ctrlRevision.get() < minRevisionToKeep) {
              logger.info("Deleting old version: " + controllerName);
              kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, controllerName);
              kubernetesContainerService.deleteAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName);
            }
          });
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("KUBERNETES_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.KUBERNETES_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.KUBERNETES_SETUP.name(), deploymentType);
    }
  }
}
