package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.awaitility.Awaitility.with;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.KubernetesReplicationControllerExecutionData.KubernetesReplicationControllerExecutionDataBuilder.aKubernetesReplicationControllerExecutionData;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.container.ContainerTask.AdvancedType.JSON;
import static software.wings.beans.container.ContainerTask.AdvancedType.YAML;
import static software.wings.beans.container.ContainerTask.CONTAINER_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.SECRET_NAME_PLACEHOLDER_REGEX;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import org.awaitility.core.ConditionTimeoutException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.KubernetesReplicationControllerExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionData;
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesReplicationControllerSetup extends ContainerServiceSetup {
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerSetup.class);

  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/KubernetesRepCtrlSetup.js

  private String replicationControllerName;
  private ServiceType serviceType;
  private Integer port;
  private Integer targetPort;
  private PortProtocol protocol;
  private String clusterIP;
  private String externalIPs;
  private String loadBalancerIP;
  private Integer nodePort;
  private String externalName;
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  /**
   * Instantiates a new state.
   */
  public KubernetesReplicationControllerSetup(String name) {
    super(name, KUBERNETES_REPLICATION_CONTROLLER_SETUP.name());
  }

  @Override
  protected StateExecutionData createService(ExecutionContext context, String serviceName, ImageDetails imageDetails,
      String appName, String envName, String clusterName, ContainerInfrastructureMapping infrastructureMapping,
      ContainerTask containerTask) {
    KubernetesConfig kubernetesConfig =
        fetchKubernetesConfig(infrastructureMapping, context.getAppId(), context.getWorkflowExecutionId());

    String rcNamePrefix = isNotEmpty(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getReplicationControllerNamePrefix(appName, serviceName, envName);
    String lastReplicationControllerName = lastReplicationController(kubernetesConfig, context, rcNamePrefix);

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;

    Map<String, String> serviceLabels = ImmutableMap.<String, String>builder()
                                            .put("app", KubernetesConvention.getLabelValue(appName))
                                            .put("service", KubernetesConvention.getLabelValue(serviceName))
                                            .put("env", KubernetesConvention.getLabelValue(envName))
                                            .build();

    Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                               .putAll(serviceLabels)
                                               .put("revision", Integer.toString(revision))
                                               .build();

    String kubernetesServiceName = KubernetesConvention.getKubernetesServiceName(rcNamePrefix);

    kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, Collections.emptyList());

    String secretName = KubernetesConvention.getKubernetesSecretName(kubernetesServiceName, imageDetails.sourceName);
    kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, Collections.emptyList(),
        createRegistrySecret(secretName, kubernetesConfig.getNamespace(), imageDetails));

    String containerServiceName = fetchContainerServiceName(kubernetesConfig, context, appName, serviceName, envName);

    ReplicationController rcDefinition = createReplicationControllerDefinition(containerTask, containerServiceName,
        controllerLabels, kubernetesConfig.getNamespace(), imageDetails, secretName, context.getServiceVariables());
    kubernetesContainerService.createController(kubernetesConfig, Collections.emptyList(), rcDefinition);

    String serviceClusterIP = null;
    String serviceLoadBalancerEndpoint = null;

    Service service =
        kubernetesContainerService.getService(kubernetesConfig, Collections.emptyList(), kubernetesServiceName);

    if (serviceType != null && serviceType != ServiceType.None) {
      Service serviceDefinition =
          createServiceDefinition(kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels);
      if (service != null) {
        // Keep the previous load balancer IP if it exists and a new one was not specified
        LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
        if (serviceType == ServiceType.LoadBalancer && isEmpty(loadBalancerIP) && loadBalancer != null
            && !loadBalancer.getIngress().isEmpty()) {
          loadBalancerIP = loadBalancer.getIngress().get(0).getIp();
          serviceDefinition =
              createServiceDefinition(kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels);
        }
      }
      service = kubernetesContainerService.createOrReplaceService(
          kubernetesConfig, Collections.emptyList(), serviceDefinition);
      serviceClusterIP = service.getSpec().getClusterIP();

      if (serviceType == ServiceType.LoadBalancer) {
        serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(kubernetesConfig, service);
      }
    } else if (service != null) {
      logger.info("Kubernetes service type set to 'None'. Deleting existing service [{}]", kubernetesServiceName);
      kubernetesContainerService.deleteService(kubernetesConfig, Collections.emptyList(), kubernetesServiceName);
    }

    logger.info("Cleaning up old versions");
    cleanup(kubernetesConfig, containerServiceName);

    String dockerImageName = imageDetails.name + ":" + imageDetails.tag;

    return aKubernetesReplicationControllerExecutionData()
        .withGkeClusterName(clusterName)
        .withKubernetesReplicationControllerName(containerServiceName)
        .withKubernetesServiceName(kubernetesServiceName)
        .withKubernetesServiceClusterIP(serviceClusterIP)
        .withKubernetesServiceLoadBalancerEndpoint(serviceLoadBalancerEndpoint)
        .withDockerImageName(dockerImageName)
        .build();
  }

  private String fetchContainerServiceName(
      KubernetesConfig kubernetesConfig, ExecutionContext context, String appName, String serviceName, String envName) {
    String rcNamePrefix = isNotEmpty(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getReplicationControllerNamePrefix(appName, serviceName, envName);
    String lastReplicationControllerName = lastReplicationController(kubernetesConfig, context, rcNamePrefix);

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;
    return KubernetesConvention.getReplicationControllerName(rcNamePrefix, revision);
  }

  private KubernetesConfig fetchKubernetesConfig(
      ContainerInfrastructureMapping infrastructureMapping, String appId, String workflowExecutionId) {
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      return gkeClusterService.getCluster(settingAttribute,
          secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), appId, workflowExecutionId),
          infrastructureMapping.getClusterName(),
          ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace());
    } else {
      return ((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig();
    }
  }

  @Override
  protected String getContainerServiceNameFromExecutionData(StateExecutionData executionData) {
    return ((KubernetesReplicationControllerExecutionData) executionData).getKubernetesReplicationControllerName();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(PhaseElement phaseElement, String serviceId,
      String appId, String workflowExecutionId, ContainerInfrastructureMapping infrastructureMapping,
      String containerServiceName) {
    return aContainerServiceElement()
        .withUuid(serviceId)
        .withName(containerServiceName)
        .withMaxInstances(getMaxInstances() == 0 ? 10 : getMaxInstances())
        .withResizeStrategy(getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy())
        .withClusterName(infrastructureMapping.getClusterName())
        .withNamespace(fetchKubernetesConfig(infrastructureMapping, appId, workflowExecutionId).getNamespace())
        .withDeploymentType(DeploymentType.KUBERNETES)
        .withInfraMappingId(phaseElement.getInfraMappingId())
        .build();
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.KUBERNETES.name();
  }

  private Secret createRegistrySecret(String secretName, String namespace, ImageDetails imageDetails) {
    String credentialData = String.format(
        DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.registryUrl, imageDetails.username, imageDetails.password);
    logger.info("Setting secret [{}]", secretName);
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

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig, Service service) {
    String loadBalancerEndpoint = null;
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null) {
      if (loadBalancer.getIngress().isEmpty()) {
        logger.info("Waiting for service [{}] load balancer to be ready.", serviceName);
        try {
          with().pollInterval(1, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).until(() -> {
            LoadBalancerStatus loadBalancerStatus =
                kubernetesContainerService.getService(kubernetesConfig, Collections.emptyList(), serviceName)
                    .getStatus()
                    .getLoadBalancer();
            boolean loadBalancerReady = !loadBalancerStatus.getIngress().isEmpty();
            if (loadBalancerReady && isNotEmpty(this.loadBalancerIP)) {
              loadBalancerReady = this.loadBalancerIP.equals(loadBalancerStatus.getIngress().get(0).getIp());
            }
            return loadBalancerReady;
          });
        } catch (ConditionTimeoutException e) {
          logger.warn(String.format("Timed out waiting for service [%s] load balancer to be ready.", serviceName), e);
          return null;
        }
        loadBalancer = kubernetesContainerService.getService(kubernetesConfig, Collections.emptyList(), serviceName)
                           .getStatus()
                           .getLoadBalancer();
      }
      LoadBalancerIngress loadBalancerIngress = loadBalancer.getIngress().get(0);
      loadBalancerEndpoint = isNotEmpty(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname()
                                                                           : loadBalancerIngress.getIp();
    }
    logger.info("Service [{}] load balancer is ready with endpoint [{}].", serviceName);
    return loadBalancerEndpoint;
  }

  private String lastReplicationController(
      KubernetesConfig kubernetesConfig, ExecutionContext context, String controllerNamePrefix) {
    ReplicationControllerList replicationControllers = kubernetesContainerService.listControllers(kubernetesConfig,
        secretManager.getEncryptionDetails(kubernetesConfig, context.getAppId(), context.getWorkflowExecutionId()));
    if (replicationControllers == null) {
      return null;
    }

    ReplicationController lastReplicationController = null;
    for (ReplicationController controller :
        replicationControllers.getItems()
            .stream()
            .filter(c
                -> c.getMetadata().getName().equals(controllerNamePrefix)
                    || c.getMetadata().getName().startsWith(controllerNamePrefix + KubernetesConvention.DOT))
            .collect(Collectors.toList())) {
      if (lastReplicationController == null
          || getRevisionFromControllerName(controller.getMetadata().getName())
              > getRevisionFromControllerName(lastReplicationController.getMetadata().getName())) {
        lastReplicationController = controller;
      }
    }
    return lastReplicationController != null ? lastReplicationController.getMetadata().getName() : null;
  }

  /**
   * Creates replication controller definition
   */
  private ReplicationController createReplicationControllerDefinition(ContainerTask containerTask,
      String replicationControllerName, Map<String, String> controllerLabels, String namespace,
      ImageDetails imageDetails, String secretName, Map<String, String> serviceVariables) {
    KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
    if (kubernetesContainerTask == null) {
      kubernetesContainerTask = new KubernetesContainerTask();
      ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
      kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = KubernetesConvention.getContainerName(imageDetails.name);

    String configTemplate;
    ContainerTask.AdvancedType type;
    if (isNotEmpty(kubernetesContainerTask.getAdvancedConfig())) {
      configTemplate = kubernetesContainerTask.getAdvancedConfig();
      type = kubernetesContainerTask.getAdvancedType();
    } else {
      configTemplate = kubernetesContainerTask.fetchYamlConfig();
      type = YAML;
    }

    String config =
        configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageDetails.name + ":" + imageDetails.tag)
            .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
            .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, secretName);

    try {
      ReplicationController rc =
          type == JSON ? (ReplicationController) KubernetesHelper.loadJson(config) : KubernetesHelper.loadYaml(config);

      KubernetesHelper.setName(rc, replicationControllerName);
      KubernetesHelper.setNamespace(rc, namespace);
      KubernetesHelper.getOrCreateLabels(rc).putAll(controllerLabels);
      rc.getSpec().setSelector(controllerLabels);
      Map<String, String> labels = rc.getSpec().getTemplate().getMetadata().getLabels();
      if (labels == null) {
        labels = new HashMap<>();
        rc.getSpec().getTemplate().getMetadata().setLabels(labels);
      }
      rc.getSpec().getTemplate().getMetadata().getLabels().putAll(controllerLabels);
      rc.getSpec().setReplicas(0);

      // Set service variables as environment variables
      if (serviceVariables != null && !serviceVariables.isEmpty()) {
        Map<String, EnvVar> serviceEnvVars =
            serviceVariables.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> new EnvVarBuilder().withName(entry.getKey()).withValue(entry.getValue()).build()));
        for (Container container : rc.getSpec().getTemplate().getSpec().getContainers()) {
          Map<String, EnvVar> envVarsMap = new HashMap<>();
          if (container.getEnv() != null) {
            container.getEnv().forEach(envVar -> envVarsMap.put(envVar.getName(), envVar));
          }
          envVarsMap.putAll(serviceEnvVars);
          container.setEnv(new ArrayList<>(envVarsMap.values()));
        }
      }
      return rc;

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
    }
  }

  /**
   * Creates service definition
   */
  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(
      String serviceName, String namespace, Map<String, String> serviceLabels) {
    ServiceSpecBuilder spec = new ServiceSpecBuilder().addToSelector(serviceLabels).withType(serviceType.name());

    if (serviceType != ServiceType.ExternalName) {
      ServicePortBuilder servicePort = new ServicePortBuilder()
                                           .withProtocol(protocol.name())
                                           .withPort(port)
                                           .withNewTargetPort()
                                           .withIntVal(targetPort)
                                           .endTargetPort();
      if (serviceType == ServiceType.NodePort && nodePort != null) {
        servicePort.withNodePort(nodePort);
      }
      spec.withPorts(ImmutableList.of(servicePort.build())); // TODO:: Allow more than one port

      if (serviceType == ServiceType.LoadBalancer && isNotEmpty(loadBalancerIP)) {
        spec.withLoadBalancerIP(loadBalancerIP);
      }

      if (serviceType == ServiceType.ClusterIP && isNotEmpty(clusterIP)) {
        spec.withClusterIP(clusterIP);
      }
    } else {
      // TODO:: fabric8 doesn't seem to support external name yet. Add here when it does.
    }

    if (isNotEmpty(externalIPs)) {
      spec.withExternalIPs(Arrays.stream(externalIPs.split(",")).map(String::trim).collect(Collectors.toList()));
    }

    return new ServiceBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(serviceName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private void cleanup(KubernetesConfig kubernetesConfig, String containerServiceName) {
    int revision = getRevisionFromControllerName(containerServiceName);
    if (revision >= KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - KEEP_N_REVISIONS + 1;
      ReplicationControllerList replicationControllers =
          kubernetesContainerService.listControllers(kubernetesConfig, Collections.emptyList());
      String controllerNamePrefix =
          KubernetesConvention.getReplicationControllerNamePrefixFromControllerName(containerServiceName);
      if (replicationControllers != null) {
        replicationControllers.getItems()
            .stream()
            .filter(c -> c.getMetadata().getName().startsWith(controllerNamePrefix) && c.getSpec().getReplicas() == 0)
            .collect(Collectors.toList())
            .forEach(rc -> {
              String controllerName = rc.getMetadata().getName();
              if (getRevisionFromControllerName(controllerName) < minRevisionToKeep) {
                logger.info("Deleting old version: " + controllerName);
                kubernetesContainerService.deleteController(kubernetesConfig, Collections.emptyList(), controllerName);
              }
            });
      }
    }
  }

  public String getReplicationControllerName() {
    return replicationControllerName;
  }

  public void setReplicationControllerName(String replicationControllerName) {
    this.replicationControllerName = replicationControllerName;
  }

  /**
   * Gets service type.
   */
  public String getServiceType() {
    return serviceType.name();
  }

  /**
   * Sets service type.
   */
  public void setServiceType(String serviceType) {
    try {
      this.serviceType = ServiceType.valueOf(serviceType);
    } catch (IllegalArgumentException e) {
      this.serviceType = ServiceType.None;
    }
  }

  public String getPort() {
    return port.toString();
  }

  public void setPort(String port) {
    this.port = Integer.parseInt(port);
  }

  public String getTargetPort() {
    return targetPort.toString();
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = Integer.parseInt(targetPort);
  }

  public String getProtocol() {
    return protocol.name();
  }

  public void setProtocol(String protocol) {
    try {
      this.protocol = PortProtocol.valueOf(protocol);
    } catch (IllegalArgumentException e) {
      this.protocol = PortProtocol.TCP;
    }
  }

  public String getClusterIP() {
    return clusterIP;
  }

  public void setClusterIP(String clusterIP) {
    this.clusterIP = clusterIP;
  }

  public String getExternalIPs() {
    return externalIPs;
  }

  public void setExternalIPs(String externalIPs) {
    this.externalIPs = externalIPs;
  }

  public String getLoadBalancerIP() {
    return loadBalancerIP;
  }

  public void setLoadBalancerIP(String loadBalancerIP) {
    this.loadBalancerIP = loadBalancerIP;
  }

  public String getNodePort() {
    return nodePort.toString();
  }

  public void setNodePort(String nodePort) {
    this.nodePort = Integer.parseInt(nodePort);
  }

  public String getExternalName() {
    return externalName;
  }

  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

  private enum ServiceType { None, ClusterIP, LoadBalancer, NodePort, ExternalName }

  private enum PortProtocol { TCP, UDP }
}
