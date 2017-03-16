package software.wings.sm.states;

import static com.google.common.base.Strings.isNullOrEmpty;
import static software.wings.api.KubernetesReplicationControllerElement.KubernetesReplicationControllerElementBuilder.aKubernetesReplicationControllerElement;
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
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.KubernetesReplicationControllerElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Service;
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
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesReplicationControllerSetup extends State {
  private enum ServiceType { ClusterIP, LoadBalancer, NodePort, ExternalName }

  private enum PortProtocol { TCP, UDP }

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
   *
   * @param name the name
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
    Environment env = workflowStandardParams.getEnv();

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (infrastructureMapping == null || !(infrastructureMapping instanceof GcpKubernetesInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    String clusterName = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getClusterName();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

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

    String containerName = Misc.normalizeExpression(imageName.toLowerCase().replace('_', '-'), "-");

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

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);

    String lastReplicationControllerName = lastReplicationController(kubernetesConfig,
        KubernetesConvention.getReplicationControllerNamePrefix(app.getName(), service.getName(), env.getName()));

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;
    String replicationControllerName =
        KubernetesConvention.getReplicationControllerName(app.getName(), service.getName(), env.getName(), revision);
    String serviceName = KubernetesConvention.getServiceName(replicationControllerName);

    Map<String, String> labels = ImmutableMap.<String, String>builder()
                                     .put("app", KubernetesConvention.getLabelValue(app.getName()))
                                     .put("service", KubernetesConvention.getLabelValue(service.getName()))
                                     .put("env", KubernetesConvention.getLabelValue(env.getName()))
                                     .put("revision", Integer.toString(revision))
                                     .build();

    kubernetesContainerService.createController(kubernetesConfig,
        new ReplicationControllerBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName(replicationControllerName)
            .addToLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withReplicas(0)
            .withSelector(labels)
            .withNewTemplate()
            .withNewMetadata()
            .addToLabels(labels)
            .endMetadata()
            .withNewSpec()
            .addToContainers(containerDefinitions.toArray(new Container[containerDefinitions.size()]))
            .addToVolumes(volumeList.toArray(new Volume[volumeList.size()]))
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());

    ServiceSpecBuilder spec = new ServiceSpecBuilder().addToSelector(labels).withType(serviceType.name());

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

    kubernetesContainerService.createService(kubernetesConfig,
        new ServiceBuilder()
            .withApiVersion("v1")
            .withNewMetadata()
            .withName(serviceName)
            .addToLabels(labels)
            .endMetadata()
            .withSpec(spec.build())
            .build());

    KubernetesReplicationControllerElement kubernetesReplicationControllerElement =
        aKubernetesReplicationControllerElement()
            .withUuid(serviceId)
            .withName(replicationControllerName)
            .withOldName(lastReplicationControllerName)
            .withClusterName(clusterName)
            .build();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(kubernetesReplicationControllerElement)
        .addNotifyElement(kubernetesReplicationControllerElement)
        .withStateExecutionData(aKubernetesReplicationControllerExecutionData()
                                    .withGkeClusterName(clusterName)
                                    .withKubernetesReplicationControllerName(replicationControllerName)
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
   * Create container definition container definition.
   *
   * @param imageName                the image name
   * @param containerName            the container name
   * @param wingsContainerDefinition the wings container definition
   * @return the container definition
   */
  public Container createContainerDefinition(
      String imageName, String containerName, KubernetesContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerBuilder containerBuilder = new ContainerBuilder().withName(containerName).withImage(imageName);

    Map<String, Quantity> limits = new HashMap<>();
    if (wingsContainerDefinition.getCpu() != null) {
      limits.put("cpu", new Quantity(wingsContainerDefinition.getCpu() + "m"));
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
                 .endVolumeMount());
    }

    return containerBuilder.build();
  }

  /**
   * Fetch artifact image name string.
   *
   * @param artifact the artifact
   * @return the string
   */
  public String fetchArtifactImageName(Artifact artifact) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (!(artifactStream instanceof DockerArtifactStream)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Containers");
    }

    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;

    return dockerArtifactStream.getImageName();
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

  public static final class KubernetesReplicationControllerSetupBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String serviceType;
    private transient GkeClusterService gkeClusterService;
    private transient KubernetesContainerService kubernetesContainerService;
    private transient SettingsService settingsService;
    private transient ServiceResourceService serviceResourceService;
    private transient InfrastructureMappingService infrastructureMappingService;
    private transient ArtifactStreamService artifactStreamService;

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

    public KubernetesReplicationControllerSetupBuilder withGkeClusterService(GkeClusterService gkeClusterService) {
      this.gkeClusterService = gkeClusterService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withKubernetesContainerService(
        KubernetesContainerService kubernetesContainerService) {
      this.kubernetesContainerService = kubernetesContainerService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withSettingsService(SettingsService settingsService) {
      this.settingsService = settingsService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withServiceResourceService(
        ServiceResourceService serviceResourceService) {
      this.serviceResourceService = serviceResourceService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withInfrastructureMappingService(
        InfrastructureMappingService infrastructureMappingService) {
      this.infrastructureMappingService = infrastructureMappingService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder withArtifactStreamService(
        ArtifactStreamService artifactStreamService) {
      this.artifactStreamService = artifactStreamService;
      return this;
    }

    public KubernetesReplicationControllerSetupBuilder but() {
      return aKubernetesReplicationControllerSetup()
          .withId(id)
          .withName(name)
          .withRequiredContextElementType(requiredContextElementType)
          .withStateType(stateType)
          .withRollback(rollback)
          .withServiceType(serviceType)
          .withGkeClusterService(gkeClusterService)
          .withKubernetesContainerService(kubernetesContainerService)
          .withSettingsService(settingsService)
          .withServiceResourceService(serviceResourceService)
          .withInfrastructureMappingService(infrastructureMappingService)
          .withArtifactStreamService(artifactStreamService);
    }

    public KubernetesReplicationControllerSetup build() {
      KubernetesReplicationControllerSetup kubernetesReplicationControllerSetup =
          new KubernetesReplicationControllerSetup(name);
      kubernetesReplicationControllerSetup.setId(id);
      kubernetesReplicationControllerSetup.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerSetup.setStateType(stateType);
      kubernetesReplicationControllerSetup.setRollback(rollback);
      kubernetesReplicationControllerSetup.setServiceType(serviceType);
      kubernetesReplicationControllerSetup.settingsService = this.settingsService;
      kubernetesReplicationControllerSetup.serviceResourceService = this.serviceResourceService;
      kubernetesReplicationControllerSetup.infrastructureMappingService = this.infrastructureMappingService;
      kubernetesReplicationControllerSetup.gkeClusterService = this.gkeClusterService;
      kubernetesReplicationControllerSetup.kubernetesContainerService = this.kubernetesContainerService;
      kubernetesReplicationControllerSetup.artifactStreamService = this.artifactStreamService;
      return kubernetesReplicationControllerSetup;
    }
  }
}
