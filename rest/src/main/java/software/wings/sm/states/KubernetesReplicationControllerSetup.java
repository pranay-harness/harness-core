package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.awaitility.Awaitility.with;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.KubernetesReplicationControllerExecutionData.KubernetesReplicationControllerExecutionDataBuilder.aKubernetesReplicationControllerExecutionData;
import static software.wings.beans.container.ContainerTask.AdvancedType.JSON;
import static software.wings.beans.container.ContainerTask.AdvancedType.YAML;
import static software.wings.beans.container.KubernetesContainerTask.CONTAINER_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.KubernetesContainerTask.DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.KubernetesContainerTask.SECRET_NAME_PLACEHOLDER_REGEX;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.KubernetesHelper;
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
import software.wings.api.ClusterElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DockerConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesReplicationControllerSetup extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerSetup.class);
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  private String replicationControllerName;
  private int maxInstances;
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
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient ServiceResourceService serviceResourceService;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient ArtifactStreamService artifactStreamService;
  @Inject @Transient private transient AwsHelperService awsHelperService;
  @Inject @Transient private transient EcrService ecrService;
  @Inject @Transient private transient EcrClassicService ecrClassicService;

  /**
   * Instantiates a new state.
   */
  public KubernetesReplicationControllerSetup(String name) {
    super(name, KUBERNETES_REPLICATION_CONTROLLER_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
    ImageDetails imageDetails = fetchArtifactDetails(artifact);

    Application app = workflowStandardParams.getApp();
    String env = workflowStandardParams.getEnv().getName();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (infrastructureMapping == null
        || !(infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
               || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();

    String clusterName;
    KubernetesConfig kubernetesConfig;
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      clusterName = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();
      if (Constants.RUNTIME.equals(clusterName)) {
        clusterName = getClusterElement(context).getName();
      }
      kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);
    } else {
      clusterName = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();
      kubernetesConfig = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig();
    }

    String rcNamePrefix = isNotEmpty(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getReplicationControllerNamePrefix(app.getName(), serviceName, env);
    String lastReplicationControllerName = lastReplicationController(kubernetesConfig, rcNamePrefix);

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;
    String replicationControllerName = KubernetesConvention.getReplicationControllerName(rcNamePrefix, revision);

    Map<String, String> serviceLabels = ImmutableMap.<String, String>builder()
                                            .put("app", KubernetesConvention.getLabelValue(app.getName()))
                                            .put("service", KubernetesConvention.getLabelValue(serviceName))
                                            .put("env", KubernetesConvention.getLabelValue(env))
                                            .build();

    Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                               .putAll(serviceLabels)
                                               .put("revision", Integer.toString(revision))
                                               .build();

    String kubernetesServiceName = KubernetesConvention.getKubernetesServiceName(rcNamePrefix);

    String secretName = KubernetesConvention.getKubernetesSecretName(kubernetesServiceName, imageDetails.sourceName);
    kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, createRegistrySecret(secretName, imageDetails));
    kubernetesContainerService.createController(kubernetesConfig,
        createReplicationControllerDefinition(replicationControllerName, controllerLabels, serviceId, imageDetails.name,
            artifact.getBuildNo(), app, secretName));

    String serviceClusterIP = null;
    String serviceLoadBalancerEndpoint = null;

    Service service = kubernetesContainerService.getService(kubernetesConfig, kubernetesServiceName);

    if (serviceType != null && serviceType != ServiceType.None) {
      Service serviceDefinition = createServiceDefinition(kubernetesServiceName, serviceLabels);
      if (service != null) {
        // Keep the previous load balancer IP if it exists and a new one was not specified
        LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
        if (serviceType == ServiceType.LoadBalancer && isEmpty(loadBalancerIP) && loadBalancer != null
            && !loadBalancer.getIngress().isEmpty()) {
          loadBalancerIP = loadBalancer.getIngress().get(0).getIp();
          serviceDefinition = createServiceDefinition(kubernetesServiceName, serviceLabels);
        }
      }
      service = kubernetesContainerService.createOrReplaceService(kubernetesConfig, serviceDefinition);
      serviceClusterIP = service.getSpec().getClusterIP();

      if (serviceType == ServiceType.LoadBalancer) {
        serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(kubernetesConfig, service);
      }
    } else if (service != null) {
      logger.info("Kubernetes service type set to 'None'. Deleting existing service [{}]", kubernetesServiceName);
      kubernetesContainerService.deleteService(kubernetesConfig, kubernetesServiceName);
    }

    ContainerServiceElement containerServiceElement = aContainerServiceElement()
                                                          .withUuid(serviceId)
                                                          .withName(replicationControllerName)
                                                          .withMaxInstances(maxInstances == 0 ? 10 : maxInstances)
                                                          .withClusterName(clusterName)
                                                          .withDeploymentType(DeploymentType.KUBERNETES)
                                                          .withInfraMappingId(phaseElement.getInfraMappingId())
                                                          .build();

    String dockerImageName = imageDetails.name + ":" + artifact.getBuildNo();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(containerServiceElement)
        .addNotifyElement(containerServiceElement)
        .withStateExecutionData(aKubernetesReplicationControllerExecutionData()
                                    .withGkeClusterName(clusterName)
                                    .withKubernetesReplicationControllerName(replicationControllerName)
                                    .withKubernetesServiceName(kubernetesServiceName)
                                    .withKubernetesServiceClusterIP(serviceClusterIP)
                                    .withKubernetesServiceLoadBalancerEndpoint(serviceLoadBalancerEndpoint)
                                    .withDockerImageName(dockerImageName)
                                    .build())
        .build();
  }

  private Secret createRegistrySecret(String secretName, ImageDetails imageDetails) {
    String credentialData = String.format(
        DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.registryUrl, imageDetails.username, imageDetails.password);
    logger.info("Setting secret [{}]", secretName);
    return new SecretBuilder()
        .withData(ImmutableMap.of(".dockercfg", new String(Base64.getEncoder().encode(credentialData.getBytes()))))
        .withNewMetadata()
        .withName(secretName)
        .withNamespace("default")
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
                kubernetesContainerService.getService(kubernetesConfig, serviceName).getStatus().getLoadBalancer();
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
        loadBalancer =
            kubernetesContainerService.getService(kubernetesConfig, serviceName).getStatus().getLoadBalancer();
      }
      LoadBalancerIngress loadBalancerIngress = loadBalancer.getIngress().get(0);
      loadBalancerEndpoint = isNotEmpty(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname()
                                                                           : loadBalancerIngress.getIp();
    }
    logger.info("Service [{}] load balancer is ready with endpoint [{}].", serviceName);
    return loadBalancerEndpoint;
  }

  private String lastReplicationController(KubernetesConfig kubernetesConfig, String controllerNamePrefix) {
    ReplicationControllerList replicationControllers = kubernetesContainerService.listControllers(kubernetesConfig);
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
          || controller.getMetadata().getCreationTimestamp().compareTo(
                 lastReplicationController.getMetadata().getCreationTimestamp())
              > 0) {
        lastReplicationController = controller;
      }
    }
    return lastReplicationController != null ? lastReplicationController.getMetadata().getName() : null;
  }

  /**
   * Creates replication controller definition
   */
  private ReplicationController createReplicationControllerDefinition(String replicationControllerName,
      Map<String, String> controllerLabels, String serviceId, String imageName, String tag, Application app,
      String secretName) {
    KubernetesContainerTask kubernetesContainerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            app.getAppId(), serviceId, DeploymentType.KUBERNETES.name());

    if (kubernetesContainerTask == null) {
      kubernetesContainerTask = new KubernetesContainerTask();
      KubernetesContainerTask.ContainerDefinition containerDefinition =
          new KubernetesContainerTask.ContainerDefinition();
      containerDefinition.setMemory(256);
      containerDefinition.setCpu(1);
      kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = KubernetesConvention.getContainerName(imageName);

    String configTemplate;
    ContainerTask.AdvancedType type;
    if (isNotEmpty(kubernetesContainerTask.getAdvancedConfig())) {
      configTemplate = kubernetesContainerTask.getAdvancedConfig();
      type = kubernetesContainerTask.getAdvancedType();
    } else {
      configTemplate = kubernetesContainerTask.fetchYamlConfig();
      type = YAML;
    }
    String config = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageName + ":" + tag)
                        .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
                        .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, secretName);

    try {
      ReplicationController rc =
          type == JSON ? (ReplicationController) KubernetesHelper.loadJson(config) : KubernetesHelper.loadYaml(config);

      KubernetesHelper.setName(rc, replicationControllerName);
      KubernetesHelper.getOrCreateLabels(rc).putAll(controllerLabels);
      rc.getSpec().setSelector(controllerLabels);
      rc.getSpec().getTemplate().getMetadata().getLabels().putAll(controllerLabels);
      rc.getSpec().setReplicas(0);
      return rc;
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
    }
  }

  /**
   * Creates service definition
   */
  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(
      String serviceName, Map<String, String> serviceLabels) {
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
        .withNamespace("default")
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private class ImageDetails {
    String name;
    String sourceName;
    String registryUrl;
    String username;
    String password;
  }

  /**
   * Fetches artifact image details
   */
  private ImageDetails fetchArtifactDetails(Artifact artifact) {
    ImageDetails imageDetails = new ImageDetails();
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    String settingId = artifactStream.getSettingId();
    if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.DOCKER.name())) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      imageDetails.name = dockerArtifactStream.getImageName();
      imageDetails.sourceName = dockerArtifactStream.getSourceName();
      DockerConfig dockerConfig = (DockerConfig) settingsService.get(settingId).getValue();
      imageDetails.registryUrl = dockerConfig.getDockerRegistryUrl();
      imageDetails.username = dockerConfig.getUsername();
      imageDetails.password = new String(dockerConfig.getPassword());
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ECR.name())) {
      EcrArtifactStream ecrArtifactStream = (EcrArtifactStream) artifactStream;
      // name should be 830767422336.dkr.ecr.us-east-1.amazonaws.com/todolist
      String imageUrl = getImageUrl(ecrArtifactStream);
      imageDetails.name = imageUrl;
      // sourceName should be todolist
      imageDetails.sourceName = ecrArtifactStream.getSourceName();
      // registryUrl should be https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
      imageDetails.registryUrl = "https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/");
      imageDetails.username = "AWS";

      SettingValue settingValue = settingsService.get(settingId).getValue();

      // All the new ECR artifact streams use cloud provider AWS settings for accesskey and secret
      if (SettingVariableTypes.AWS.name().equals(settingValue.getType())) {
        AwsConfig awsConfig = (AwsConfig) settingsService.get(settingId).getValue();
        imageDetails.password = awsHelperService.getAmazonEcrAuthToken(imageUrl.substring(0, imageUrl.indexOf('.')),
            ecrArtifactStream.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      } else {
        // There is a point when old ECR artifact streams would be using the old ECR Artifact Server definition until
        // migration happens. The deployment code handles both the cases.
        EcrConfig ecrConfig = (EcrConfig) settingsService.get(settingId).getValue();
        imageDetails.password = awsHelperService.getAmazonEcrAuthToken(ecrConfig);
      }
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.GCR.name())) {
      GcrArtifactStream gcrArtifactStream = (GcrArtifactStream) artifactStream;
      String imageName = gcrArtifactStream.getRegistryHostName() + "/" + gcrArtifactStream.getDockerImageName();
      imageDetails.name = imageName;
      imageDetails.sourceName = imageName;
      imageDetails.registryUrl = imageName;
    } else if (artifactStream.getArtifactStreamType().equals(ArtifactStreamType.ARTIFACTORY.name())) {
      ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
      imageDetails.name = artifactoryArtifactStream.getImageName();
      imageDetails.sourceName = artifactoryArtifactStream.getSourceName();
      ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingsService.get(settingId).getValue();
      imageDetails.registryUrl = artifactoryConfig.getArtifactoryUrl();
      imageDetails.username = artifactoryConfig.getUsername();
      imageDetails.password = new String(artifactoryConfig.getPassword());
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Kubernetes");
    }
    return imageDetails;
  }

  private String getImageUrl(EcrArtifactStream ecrArtifactStream) {
    SettingAttribute settingAttribute = settingsService.get(ecrArtifactStream.getSettingId());
    SettingValue value = settingAttribute.getValue();
    if (SettingVariableTypes.AWS.name().equals(value.getType())) {
      AwsConfig awsConfig = (AwsConfig) value;
      return ecrService.getEcrImageUrl(awsConfig, ecrArtifactStream.getRegion(), ecrArtifactStream);
    } else {
      EcrConfig ecrConfig = (EcrConfig) value;
      return ecrClassicService.getEcrImageUrl(ecrConfig, ecrArtifactStream);
    }
  }

  private ClusterElement getClusterElement(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    return context.<ClusterElement>getContextElementList(ContextElementType.CLUSTER)
        .stream()
        .filter(clusterElement -> phaseElement.getInfraMappingId().equals(clusterElement.getInfraMappingId()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getReplicationControllerName() {
    return replicationControllerName;
  }

  public void setReplicationControllerName(String replicationControllerName) {
    this.replicationControllerName = replicationControllerName;
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
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

  public static final class KubernetesReplicationControllerSetupBuilder {
    private String id;
    private String parentId;
    private String name;
    private boolean rollback;
    private String replicationControllerName;
    private int maxInstances;
    private String serviceType;
    private String port;
    private String targetPort;
    private String protocol;
    private String clusterIP;
    private String externalIPs;
    private String loadBalancerIP;
    private String nodePort;
    private String externalName;

    private KubernetesReplicationControllerSetupBuilder() {}

    public static KubernetesReplicationControllerSetupBuilder aKubernetesReplicationControllerSetup() {
      return new KubernetesReplicationControllerSetupBuilder();
    }

    public KubernetesReplicationControllerSetupBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withParentId(String parentId) {
      this.parentId = parentId;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withReplicationControllerName(String replicationControllerName) {
      this.replicationControllerName = replicationControllerName;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withMaxInstances(int maxInstances) {
      this.maxInstances = maxInstances;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withServiceType(String serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withPort(String port) {
      this.port = port;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withTargetPort(String targetPort) {
      this.targetPort = targetPort;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withProtocol(String protocol) {
      this.protocol = protocol;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withClusterIP(String clusterIP) {
      this.clusterIP = clusterIP;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withExternalIPs(String externalIPs) {
      this.externalIPs = externalIPs;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withLoadBalancerIP(String loadBalancerIP) {
      this.loadBalancerIP = loadBalancerIP;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withNodePort(String nodePort) {
      this.nodePort = nodePort;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withExternalName(String externalName) {
      this.externalName = externalName;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder but() {
      return aKubernetesReplicationControllerSetup()
          .withId(id)
          .withParentId(parentId)
          .withName(name)
          .withRollback(rollback)
          .withReplicationControllerName(replicationControllerName)
          .withMaxInstances(maxInstances)
          .withServiceType(serviceType)
          .withPort(port)
          .withTargetPort(targetPort)
          .withProtocol(protocol)
          .withClusterIP(clusterIP)
          .withExternalIPs(externalIPs)
          .withLoadBalancerIP(loadBalancerIP)
          .withNodePort(nodePort)
          .withExternalName(externalName);
    }

    public KubernetesReplicationControllerSetup build() {
      KubernetesReplicationControllerSetup kubernetesReplicationControllerSetup =
          new KubernetesReplicationControllerSetup(name);
      kubernetesReplicationControllerSetup.setId(id);
      kubernetesReplicationControllerSetup.setParentId(parentId);
      kubernetesReplicationControllerSetup.setRollback(rollback);
      kubernetesReplicationControllerSetup.setReplicationControllerName(replicationControllerName);
      kubernetesReplicationControllerSetup.setMaxInstances(maxInstances);
      kubernetesReplicationControllerSetup.setServiceType(serviceType);
      kubernetesReplicationControllerSetup.setPort(port);
      kubernetesReplicationControllerSetup.setTargetPort(targetPort);
      kubernetesReplicationControllerSetup.setProtocol(protocol);
      kubernetesReplicationControllerSetup.setClusterIP(clusterIP);
      kubernetesReplicationControllerSetup.setExternalIPs(externalIPs);
      kubernetesReplicationControllerSetup.setLoadBalancerIP(loadBalancerIP);
      kubernetesReplicationControllerSetup.setNodePort(nodePort);
      kubernetesReplicationControllerSetup.setExternalName(externalName);
      return kubernetesReplicationControllerSetup;
    }
  }
}
