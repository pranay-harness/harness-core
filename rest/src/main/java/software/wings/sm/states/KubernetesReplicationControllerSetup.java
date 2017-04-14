package software.wings.sm.states;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.awaitility.Awaitility.with;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.KubernetesReplicationControllerExecutionData.KubernetesReplicationControllerExecutionDataBuilder.aKubernetesReplicationControllerExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ClusterElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerSetup extends State {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesReplicationControllerSetup.class);
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
    String imageName = fetchArtifactImageName(artifact);

    Application app = workflowStandardParams.getApp();
    String env = workflowStandardParams.getEnv().getName();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (infrastructureMapping == null || !(infrastructureMapping instanceof GcpKubernetesInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String serviceName = serviceResourceService.get(app.getUuid(), serviceId).getName();

    String clusterName = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();
    if (Constants.RUNTIME.equals(clusterName)) {
      clusterName = getClusterElement(context).getName();
    }

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);

    String lastReplicationControllerName = lastReplicationController(
        kubernetesConfig, KubernetesConvention.getReplicationControllerNamePrefix(app.getName(), serviceName, env));

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;
    String replicationControllerName =
        KubernetesConvention.getReplicationControllerName(app.getName(), serviceName, env, revision);

    Map<String, String> serviceLabels = ImmutableMap.<String, String>builder()
                                            .put("app", KubernetesConvention.getLabelValue(app.getName()))
                                            .put("service", KubernetesConvention.getLabelValue(serviceName))
                                            .put("env", KubernetesConvention.getLabelValue(env))
                                            .build();

    Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                               .putAll(serviceLabels)
                                               .put("revision", Integer.toString(revision))
                                               .build();

    kubernetesContainerService.createController(kubernetesConfig,
        createReplicationControllerDefinition(replicationControllerName, controllerLabels, serviceId, imageName, app));

    String kubernetesServiceName = null;
    String serviceClusterIP = null;
    String serviceLoadBalancerIP = null;

    if (serviceType != null && serviceType != ServiceType.None) {
      kubernetesServiceName = KubernetesConvention.getKubernetesServiceName(app.getName(), serviceName, env);
      Service service = kubernetesContainerService.getService(kubernetesConfig, kubernetesServiceName);
      if (service == null) {
        logger.info("Kubernetes service {} does not exist. Creating.", kubernetesServiceName);
        service = kubernetesContainerService.createService(
            kubernetesConfig, createServiceDefinition(kubernetesServiceName, serviceLabels));
      }
      serviceClusterIP = service.getSpec().getClusterIP();
      LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();

      if (loadBalancer != null) {
        if (loadBalancer.getIngress().isEmpty()) {
          String finalKubernetesServiceName = kubernetesServiceName;

          with()
              .pollInterval(1, TimeUnit.SECONDS)
              .await()
              .atMost(60, TimeUnit.SECONDS)
              .until(()
                         -> !kubernetesContainerService.getService(kubernetesConfig, finalKubernetesServiceName)
                                 .getStatus()
                                 .getLoadBalancer()
                                 .getIngress()
                                 .isEmpty());

          loadBalancer = kubernetesContainerService.getService(kubernetesConfig, finalKubernetesServiceName)
                             .getStatus()
                             .getLoadBalancer();
        }
        serviceLoadBalancerIP = loadBalancer.getIngress().get(0).getIp();
      }
    }

    ContainerServiceElement containerServiceElement = aContainerServiceElement()
                                                          .withUuid(serviceId)
                                                          .withName(replicationControllerName)
                                                          .withOldName(lastReplicationControllerName)
                                                          .withClusterName(clusterName)
                                                          .withDeploymentType(DeploymentType.KUBERNETES)
                                                          .withInfraMappingId(phaseElement.getInfraMappingId())
                                                          .build();

    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(containerServiceElement)
        .addNotifyElement(containerServiceElement)
        .withStateExecutionData(aKubernetesReplicationControllerExecutionData()
                                    .withGkeClusterName(clusterName)
                                    .withKubernetesReplicationControllerName(replicationControllerName)
                                    .withKubernetesServiceName(kubernetesServiceName)
                                    .withKubernetesServiceClusterIP(serviceClusterIP)
                                    .withKubernetesServiceLoadBalancerIP(serviceLoadBalancerIP)
                                    .withDockerImageName(imageName)
                                    .build())
        .build();
  }

  private String lastReplicationController(KubernetesConfig kubernetesConfig, String controllerNamePrefix) {
    ReplicationControllerList replicationControllers = kubernetesContainerService.listControllers(kubernetesConfig);
    if (replicationControllers == null) {
      return null;
    }
    List<ReplicationController> replicationControllerList =
        replicationControllers.getItems()
            .stream()
            .filter(controller
                -> controller.getMetadata().getName().startsWith(controllerNamePrefix)
                    && controller.getSpec().getReplicas() > 0)
            .collect(Collectors.toList());

    ReplicationController lastReplicationController = null;
    for (ReplicationController controller : replicationControllerList) {
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
      Map<String, String> controllerLabels, String serviceId, String imageName, Application app) {
    KubernetesContainerTask kubernetesContainerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            app.getAppId(), serviceId, DeploymentType.KUBERNETES.name());

    if (kubernetesContainerTask == null) {
      kubernetesContainerTask = new KubernetesContainerTask();
      KubernetesContainerTask.ContainerDefinition containerDefinition =
          new KubernetesContainerTask.ContainerDefinition();
      containerDefinition.setMemory(256);
      kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = KubernetesConvention.getContainerName(imageName);

    List<Container> containerDefinitions =
        kubernetesContainerTask.getContainerDefinitions()
            .stream()
            .map(containerDefinition -> createContainerDefinition(imageName, containerName, containerDefinition))
            .collect(Collectors.toList());

    List<Volume> volumeList = new ArrayList<>();
    kubernetesContainerTask.getContainerDefinitions().forEach(containerDefinition -> {
      if (containerDefinition.getStorageConfigurations() != null) {
        volumeList.addAll(
            containerDefinition.getStorageConfigurations()
                .stream()
                .map(storageConfiguration
                    -> new VolumeBuilder()
                           .withName(KubernetesConvention.getVolumeName(storageConfiguration.getHostSourcePath()))
                           .withHostPath(new HostPathVolumeSource(storageConfiguration.getHostSourcePath()))
                           .build())
                .collect(Collectors.toList()));
      }
    });

    return new ReplicationControllerBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(replicationControllerName)
        .addToLabels(controllerLabels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(0)
        .withSelector(controllerLabels)
        .withNewTemplate()
        .withNewMetadata()
        .addToLabels(controllerLabels)
        .endMetadata()
        .withNewSpec()
        .addToContainers(containerDefinitions.toArray(new Container[containerDefinitions.size()]))
        .addToVolumes(volumeList.toArray(new Volume[volumeList.size()]))
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
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

      if (serviceType == ServiceType.LoadBalancer && !isNullOrEmpty(loadBalancerIP)) {
        spec.withLoadBalancerIP(loadBalancerIP);
      }

      if (serviceType == ServiceType.ClusterIP && !isNullOrEmpty(clusterIP)) {
        spec.withClusterIP(clusterIP);
      }
    } else {
      // TODO:: fabric8 doesn't seem to support external name yet. Add here when it does.
    }

    if (!isNullOrEmpty(externalIPs)) {
      spec.withExternalIPs(Arrays.stream(externalIPs.split(",")).map(String::trim).collect(Collectors.toList()));
    }

    return new ServiceBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(serviceName)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  /**
   * Creates container definition
   */
  private Container createContainerDefinition(
      String imageName, String containerName, KubernetesContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerBuilder containerBuilder = new ContainerBuilder().withName(containerName).withImage(imageName);

    Map<String, Quantity> limits = new HashMap<>();
    if (wingsContainerDefinition.getCpu() != null) {
      limits.put("cpu", new Quantity(wingsContainerDefinition.getCpu().toString()));
    }

    if (wingsContainerDefinition.getMemory() != null) {
      limits.put("memory", new Quantity(wingsContainerDefinition.getMemory() + "Mi"));
    }

    if (!limits.isEmpty()) {
      containerBuilder.withNewResources().withLimits(limits).endResources();
    }

    if (wingsContainerDefinition.getPortMappings() != null) {
      wingsContainerDefinition.getPortMappings().forEach(portMapping
          -> containerBuilder.addNewPort()
                 .withContainerPort(portMapping.getContainerPort())
                 .withHostPort(portMapping.getHostPort())
                 .withProtocol("TCP")
                 .endPort());
    }

    if (wingsContainerDefinition.getCommands() != null) {
      wingsContainerDefinition.getCommands().forEach(command -> {
        if (!command.trim().isEmpty()) {
          containerBuilder.withCommand(command.trim());
        }
      });
    }

    if (wingsContainerDefinition.getEnvironmentVariables() != null) {
      wingsContainerDefinition.getEnvironmentVariables().forEach(
          envVar -> containerBuilder.addNewEnv().withName(envVar.getName()).withValue(envVar.getValue()).endEnv());
    }

    if (wingsContainerDefinition.getLogConfiguration() != null) {
      KubernetesContainerTask.LogConfiguration wingsLogConfiguration = wingsContainerDefinition.getLogConfiguration();
      // TODO:: Check about kubernetes logs.  See https://kubernetes.io/docs/concepts/clusters/logging/
    }

    if (wingsContainerDefinition.getStorageConfigurations() != null) {
      wingsContainerDefinition.getStorageConfigurations().forEach(storageConfiguration
          -> containerBuilder.addNewVolumeMount()
                 .withName(KubernetesConvention.getVolumeName(storageConfiguration.getHostSourcePath()))
                 .withMountPath(storageConfiguration.getContainerPath())
                 .withReadOnly(storageConfiguration.isReadonly())
                 .endVolumeMount());
    }

    return containerBuilder.build();
  }

  /**
   * Fetches artifact image name string
   */
  private String fetchArtifactImageName(Artifact artifact) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (!(artifactStream instanceof DockerArtifactStream)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Containers");
    }

    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;

    return dockerArtifactStream.getImageName();
  }

  private ClusterElement getClusterElement(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    List<ClusterElement> clusterElements = context.getContextElementList(ContextElementType.CLUSTER);

    return clusterElements.stream()
        .filter(clusterElement -> phaseElement.getInfraMappingId().equals(clusterElement.getInfraMappingId()))
        .findFirst()
        .map(element -> element)
        .orElse(null);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

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
      this.serviceType = ServiceType.ClusterIP;
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
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String serviceType;

    private KubernetesReplicationControllerSetupBuilder() {}

    public static KubernetesReplicationControllerSetupBuilder aKubernetesReplicationControllerSetup() {
      return new KubernetesReplicationControllerSetupBuilder();
    }

    public KubernetesReplicationControllerSetupBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withRequiredContextElementType(
        ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withServiceType(String serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder but() {
      return aKubernetesReplicationControllerSetup()
          .withId(id)
          .withName(name)
          .withRequiredContextElementType(requiredContextElementType)
          .withStateType(stateType)
          .withRollback(rollback)
          .withServiceType(serviceType);
    }

    public KubernetesReplicationControllerSetup build() {
      KubernetesReplicationControllerSetup kubernetesReplicationControllerSetup =
          new KubernetesReplicationControllerSetup(name);
      kubernetesReplicationControllerSetup.setId(id);
      kubernetesReplicationControllerSetup.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerSetup.setStateType(stateType);
      kubernetesReplicationControllerSetup.setRollback(rollback);
      kubernetesReplicationControllerSetup.setServiceType(serviceType);
      return kubernetesReplicationControllerSetup;
    }
  }
}
