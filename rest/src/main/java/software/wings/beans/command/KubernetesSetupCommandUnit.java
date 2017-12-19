package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.awaitility.Awaitility.with;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import org.awaitility.core.ConditionTimeoutException;
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

  public KubernetesSetupCommandUnit() {
    super(CommandUnitType.KUBERNETES_SETUP);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected String executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerSetupParams containerSetupParams,
      Map<String, String> serviceVariables, ExecutionLogCallback executionLogCallback) {
    KubernetesSetupParams setupParams = (KubernetesSetupParams) containerSetupParams;

    KubernetesConfig kubernetesConfig;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
    } else {
      kubernetesConfig = gkeClusterService.getCluster(
          cloudProviderSetting, encryptedDataDetails, setupParams.getClusterName(), setupParams.getNamespace());
    }

    int revision = KubernetesConvention
                       .getRevisionFromControllerName(lastReplicationController(
                           kubernetesConfig, encryptedDataDetails, setupParams.getRcNamePrefix()))
                       .orElse(-1)
        + 1;

    String kubernetesServiceName = KubernetesConvention.getKubernetesServiceName(setupParams.getRcNamePrefix());

    kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, emptyList());

    String secretName = KubernetesConvention.getKubernetesSecretName(setupParams.getImageDetails().getRegistryUrl());
    kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, emptyList(),
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
        ? setupParams.getRcNamePrefix()
        : KubernetesConvention.getControllerName(setupParams.getRcNamePrefix(), revision);

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

    HasMetadata controllerDefinition =
        createKubernetesControllerDefinition(kubernetesContainerTask, containerServiceName, controllerLabels,
            kubernetesConfig.getNamespace(), setupParams.getImageDetails(), secretName, serviceVariables);
    kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails, controllerDefinition);

    String serviceClusterIP = null;
    String serviceLoadBalancerEndpoint = null;

    Service service = kubernetesContainerService.getService(kubernetesConfig, emptyList(), kubernetesServiceName);

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
      service = kubernetesContainerService.createOrReplaceService(kubernetesConfig, emptyList(), serviceDefinition);
      serviceClusterIP = service.getSpec().getClusterIP();

      if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer) {
        serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(
            kubernetesConfig, service, setupParams.getLoadBalancerIP(), executionLogCallback);
      }
    } else if (service != null) {
      executionLogCallback.saveExecutionLog(
          "Kubernetes service type set to 'None'. Deleting existing service " + kubernetesServiceName, LogLevel.INFO);
      kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
    }

    executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
    cleanup(kubernetesConfig, encryptedDataDetails, containerServiceName);

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
      int desiredCount = kubernetesContainerService.getNodes(kubernetesConfig, encryptedDataDetails).getItems().size();
      List<ContainerInfo> containerInfos = kubernetesContainerService.getContainerInfosWhenReady(
          kubernetesConfig, encryptedDataDetails, containerServiceName, desiredCount, executionLogCallback);

      boolean allContainersSuccess =
          containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
      if (containerInfos.size() != desiredCount || !allContainersSuccess) {
        StringBuilder msg = new StringBuilder();
        if (containerInfos.size() != desiredCount) {
          String message = String.format("Expected data for %d container%s but got %d", desiredCount,
              containerInfos.size() == 1 ? "" : "s", containerInfos.size());
          executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
          msg.append(message);
        }
        if (!allContainersSuccess) {
          List<ContainerInfo> failed = containerInfos.stream()
                                           .filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS)
                                           .collect(Collectors.toList());
          String message =
              String.format("The following container%s did not have success status: %s", failed.size() == 1 ? "" : "s",
                  failed.stream().map(ContainerInfo::getContainerId).collect(Collectors.toList()));
          executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
          msg.append(message);
        }
        throw new WingsException(
            ErrorCode.DEFAULT_ERROR_CODE, "message", "DaemonSet pods failed to reach desired count. " + msg.toString());
      }
      containerInfos.forEach(info
          -> executionLogCallback.saveExecutionLog("DaemonSet container ID: " + info.getContainerId(), LogLevel.INFO));
    }
    return containerServiceName;
  }

  private Secret createRegistrySecret(
      String secretName, String namespace, ImageDetails imageDetails, ExecutionLogCallback executionLogCallback) {
    String credentialData = String.format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
    executionLogCallback.saveExecutionLog("Setting secret " + secretName, LogLevel.INFO);
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

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig, Service service, String loadBalancerIP,
      ExecutionLogCallback executionLogCallback) {
    String loadBalancerEndpoint = null;
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null) {
      if (loadBalancer.getIngress().isEmpty()) {
        executionLogCallback.saveExecutionLog(
            "Waiting for service " + serviceName + " load balancer to be ready.", LogLevel.INFO);
        try {
          with().pollInterval(1, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).until(() -> {
            LoadBalancerStatus loadBalancerStatus =
                kubernetesContainerService.getService(kubernetesConfig, emptyList(), serviceName)
                    .getStatus()
                    .getLoadBalancer();
            boolean loadBalancerReady = !loadBalancerStatus.getIngress().isEmpty();
            if (loadBalancerReady && isNotEmpty(loadBalancerIP)) {
              loadBalancerReady = loadBalancerIP.equals(loadBalancerStatus.getIngress().get(0).getIp());
            }
            return loadBalancerReady;
          });
        } catch (ConditionTimeoutException e) {
          executionLogCallback.saveExecutionLog(
              String.format("Timed out waiting for service [%s] load balancer to be ready.", serviceName),
              LogLevel.INFO);
          return null;
        }
        loadBalancer = kubernetesContainerService.getService(kubernetesConfig, emptyList(), serviceName)
                           .getStatus()
                           .getLoadBalancer();
      }
      LoadBalancerIngress loadBalancerIngress = loadBalancer.getIngress().get(0);
      loadBalancerEndpoint = isNotEmpty(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname()
                                                                           : loadBalancerIngress.getIp();
    }
    executionLogCallback.saveExecutionLog(
        String.format("Service [%s] load balancer is ready with endpoint [%s].", serviceName, loadBalancerEndpoint),
        LogLevel.INFO);
    return loadBalancerEndpoint;
  }

  private String lastReplicationController(
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

  /**
   * Creates controller definition
   */
  private HasMetadata createKubernetesControllerDefinition(KubernetesContainerTask kubernetesContainerTask,
      String replicationControllerName, Map<String, String> controllerLabels, String namespace,
      ImageDetails imageDetails, String secretName, Map<String, String> serviceVariables) {
    String containerName = KubernetesConvention.getContainerName(imageDetails.getName());
    String imageNameTag = imageDetails.getName() + ":" + imageDetails.getTag();

    HasMetadata kubernetesObj = kubernetesContainerTask.createController(containerName, imageNameTag, secretName);

    KubernetesHelper.setName(kubernetesObj, replicationControllerName);
    KubernetesHelper.setNamespace(kubernetesObj, namespace);
    KubernetesHelper.getOrCreateLabels(kubernetesObj).putAll(controllerLabels);

    configureTypeSpecificSpecs(controllerLabels, kubernetesObj, serviceVariables);

    return kubernetesObj;
  }

  private void configureTypeSpecificSpecs(
      Map<String, String> controllerLabels, HasMetadata kubernetesObj, Map<String, String> serviceVariables) {
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
      if (serviceVariables != null && !serviceVariables.isEmpty()) {
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

  /**
   * Creates service definition
   */
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
      spec.withExternalIPs(
          Arrays.stream(setupParams.getExternalIPs().split(",")).map(String::trim).collect(Collectors.toList()));
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
