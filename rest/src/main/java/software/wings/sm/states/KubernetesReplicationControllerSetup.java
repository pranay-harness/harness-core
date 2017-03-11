package software.wings.sm.states;

import static software.wings.api.KubernetesReplicationControllerElement.KubernetesReplicationControllerElementBuilder.aKubernetesReplicationControllerElement;
import static software.wings.api.KubernetesReplicationControllerExecutionData.KubernetesReplicationControllerExecutionDataBuilder.aKubernetesReplicationControllerExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
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
import software.wings.stencils.EnumData;
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/1/17
 * TODO(brett): Implement
 */
public class KubernetesReplicationControllerSetup extends State {
  @Attributes(title = "Load Balancer")
  @EnumData(enumDataProvider = LoadBalancerDataProvider.class)
  private String loadBalancerSettingId;

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

    Service service = serviceResourceService.get(app.getAppId(), serviceId);
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

    String containerName = imageName.replace('/', '_');

    Map<String, String> labels = new HashMap<>();
    if (kubernetesContainerTask.getLabels() != null) {
      kubernetesContainerTask.getLabels().forEach(label -> labels.put(label.getName(), label.getValue()));
    }

    List<Container> containerDefinitions =
        kubernetesContainerTask.getContainerDefinitions()
            .stream()
            .map(containerDefinition -> createContainerDefinition(imageName, containerName, containerDefinition))
            .collect(Collectors.toList());

    List<Volume> volumeList = new ArrayList<>();
    kubernetesContainerTask.getContainerDefinitions().forEach(containerDefinition
        -> volumeList.addAll(
            containerDefinition.getStorageConfigurations()
                .stream()
                .map(storageConfiguration
                    -> new VolumeBuilder()
                           .withName(storageConfiguration.getHostSourcePath().replace('/', '_'))
                           .withHostPath(new HostPathVolumeSource(storageConfiguration.getHostSourcePath()))
                           .build())
                .collect(Collectors.toList())));

    /*
    TODO:: Setup load balancer service
    SettingAttribute loadBalancerSetting = settingsService.get(loadBalancerSettingId);

    if (loadBalancerSetting == null ||
    !loadBalancerSetting.getValue().getType().equals(SettingVariableTypes.ALB.name())) { throw new
    WingsException(ErrorCode.INVALID_REQUEST, "message", "Load balancer is not of ALB type");
    }
    ApplicationLoadBalancerConfig albConfig = (ApplicationLoadBalancerConfig) loadBalancerSetting.getValue();
    */

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(computeProviderSetting, clusterName);

    String lastReplicationControllerName = lastReplicationController(kubernetesConfig,
        KubernetesConvention.getReplicationControllerNamePrefix(app.getName(), service.getName(), env.getName()));

    String replicationControllerName =
        KubernetesConvention.getReplicationControllerName(app.getName(), service.getName(), env.getName(),
            KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1);

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
      containerBuilder.withCommand(wingsContainerDefinition.getCommands());
    }

    if (wingsContainerDefinition.getArguments() != null) {
      containerBuilder.withArgs(wingsContainerDefinition.getArguments());
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
                 .withName(storageConfiguration.getHostSourcePath().replace('/', '_'))
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
   * Gets load balancer setting id.
   *
   * @return the load balancer setting id
   */
  public String getLoadBalancerSettingId() {
    return loadBalancerSettingId;
  }

  /**
   * Sets load balancer setting id.
   *
   * @param loadBalancerSettingId the load balancer setting id
   */
  public void setLoadBalancerSettingId(String loadBalancerSettingId) {
    this.loadBalancerSettingId = loadBalancerSettingId;
  }

  public static final class KubernetesReplicationControllerSetupBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private boolean rollback;
    private String loadBalancerSettingId;
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

    public KubernetesReplicationControllerSetupBuilder withLoadBalancerSettingId(String loadBalancerSettingId) {
      this.loadBalancerSettingId = loadBalancerSettingId;
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
          .withLoadBalancerSettingId(loadBalancerSettingId)
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
      kubernetesReplicationControllerSetup.setLoadBalancerSettingId(loadBalancerSettingId);
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
