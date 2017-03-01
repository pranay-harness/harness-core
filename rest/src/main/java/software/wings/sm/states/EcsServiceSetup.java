package software.wings.sm.states;

import static software.wings.api.EcsServiceElement.EcsServiceElementBuilder.anEcsServiceElement;
import static software.wings.api.EcsServiceExecutionData.EcsServiceExecutionDataBuilder.anEcsServiceExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.aws.AwsClusterService;
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
import software.wings.utils.ECSConvention;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class EcsServiceSetup extends State {
  @Attributes(title = "Load Balancer")
  @EnumData(enumDataProvider = LoadBalancerDataProvider.class)
  private String loadBalancerSettingId;

  @Inject @Transient private transient AwsClusterService awsClusterService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient ArtifactStreamService artifactStreamService;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
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
    if (infrastructureMapping == null || !(infrastructureMapping instanceof EcsInfrastructureMapping)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid infrastructure type");
    }

    String clusterName = ((EcsInfrastructureMapping) infrastructureMapping).getClusterName();

    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    EcsContainerTask ecsContainerTask = (EcsContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
        app.getAppId(), serviceId, DeploymentType.ECS.name());

    if (ecsContainerTask == null) {
      ecsContainerTask = new EcsContainerTask();
      EcsContainerTask.ContainerDefinition containerDefinition = new EcsContainerTask.ContainerDefinition();
      containerDefinition.setMemory(256);
      ecsContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = imageName.replace('/', '_');
    Integer containerPort = 8080; // TODO: don't hardcode read from config

    List<ContainerDefinition> containerDefinitions =
        ecsContainerTask.getContainerDefinitions()
            .stream()
            .map(containerDefinition -> createContainerDefinition(imageName, containerName, containerDefinition))
            .collect(Collectors.toList());

    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(containerDefinitions)
            .withFamily(ECSConvention.getTaskFamily(app.getName(), service.getName(), env.getName()));

    TaskDefinition taskDefinition = awsClusterService.createTask(computeProviderSetting, registerTaskDefinitionRequest);

    /*

    SettingAttribute loadBalancerSetting = settingsService.get(loadBalancerSettingId);

    if (loadBalancerSetting == null ||
    !loadBalancerSetting.getValue().getType().equals(SettingVariableTypes.ALB.name())) { throw new
    WingsException(ErrorCode.INVALID_REQUEST, "message", "Load balancer is not of ALB type");
    }
    ApplicationLoadBalancerConfig albConfig = (ApplicationLoadBalancerConfig) loadBalancerSetting.getValue();
*/
    String ecsServiceName = ECSConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision());

    String lastEcsServiceName = lastECSService(
        computeProviderSetting, clusterName, ECSConvention.getServiceNamePrefix(taskDefinition.getFamily()));

    awsClusterService.createService(computeProviderSetting,
        new CreateServiceRequest()
            .withServiceName(ecsServiceName)
            .withCluster(clusterName)
            .withDesiredCount(0)
            .withDeploymentConfiguration(
                new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision()));

    EcsServiceElement ecsServiceElement = anEcsServiceElement()
                                              .withUuid(serviceId)
                                              .withName(ecsServiceName)
                                              .withOldName(lastEcsServiceName)
                                              .withClusterName(clusterName)
                                              .build();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addContextElement(ecsServiceElement)
        .addNotifyElement(ecsServiceElement)
        .withStateExecutionData(anEcsServiceExecutionData()
                                    .withEcsClusterName(clusterName)
                                    .withEcsServiceName(ecsServiceName)
                                    .withDockerImageName(imageName)
                                    .build())
        .build();
  }

  private String lastECSService(SettingAttribute computeProviderSetting, String clusterName, String serviceNamePrefix) {
    List<com.amazonaws.services.ecs.model.Service> services =
        awsClusterService.getServices(computeProviderSetting, clusterName);
    if (services == null) {
      return null;
    }
    List<com.amazonaws.services.ecs.model.Service> serviceList =
        services.stream()
            .filter(
                service -> (service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0))
            .collect(Collectors.toList());

    com.amazonaws.services.ecs.model.Service lastECSService = null;
    for (com.amazonaws.services.ecs.model.Service service : serviceList) {
      if (lastECSService == null || service.getCreatedAt().compareTo(lastECSService.getCreatedAt()) > 0) {
        lastECSService = service;
      }
    }
    return lastECSService != null ? lastECSService.getServiceName() : null;
  }

  /**
   * Create container definition container definition.
   *
   * @param imageName                the image name
   * @param containerName            the container name
   * @param wingsContainerDefinition the wings container definition
   * @return the container definition
   */
  private ContainerDefinition createContainerDefinition(
      String imageName, String containerName, EcsContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerDefinition containerDefinition = new ContainerDefinition().withName(containerName).withImage(imageName);

    if (wingsContainerDefinition.getCpu() != null) {
      containerDefinition.setCpu(wingsContainerDefinition.getCpu());
    }

    if (wingsContainerDefinition.getMemory() != null) {
      containerDefinition.setMemory(wingsContainerDefinition.getMemory());
    }

    if (wingsContainerDefinition.getPortMappings() != null) {
      List<PortMapping> portMappings = wingsContainerDefinition.getPortMappings()
                                           .stream()
                                           .map(portMapping
                                               -> new PortMapping()
                                                      .withContainerPort(portMapping.getContainerPort())
                                                      .withHostPort(portMapping.getHostPort())
                                                      .withProtocol(TransportProtocol.Tcp))
                                           .collect(Collectors.toList());
      containerDefinition.setPortMappings(portMappings);
    }

    if (wingsContainerDefinition.getCommands() != null) {
      containerDefinition.setCommand(wingsContainerDefinition.getCommands());
    }

    if (wingsContainerDefinition.getLogConfiguration() != null) {
      EcsContainerTask.LogConfiguration wingsLogConfiguration = wingsContainerDefinition.getLogConfiguration();
      LogConfiguration logConfiguration = new LogConfiguration().withLogDriver(wingsLogConfiguration.getLogDriver());
      wingsLogConfiguration.getOptions().forEach(
          logOption -> logConfiguration.addOptionsEntry(logOption.getKey(), logOption.getValue()));
      containerDefinition.setLogConfiguration(logConfiguration);
    }

    if (wingsContainerDefinition.getStorageConfigurations() != null) {
      // TODO:: fill volume amd mount points here
    }

    return containerDefinition;
  }

  /**
   * Fetch artifact image name string.
   *
   * @param artifact the artifact
   * @return the string
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

  public static final class EcsServiceSetupBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private boolean rollback;
    private String loadBalancerSettingId;
    private transient AwsClusterService awsClusterService;
    private transient SettingsService settingsService;
    private transient ServiceResourceService serviceResourceService;
    private transient InfrastructureMappingService infrastructureMappingService;
    private transient ArtifactStreamService artifactStreamService;

    private EcsServiceSetupBuilder(String name) {
      this.name = name;
    }

    public static EcsServiceSetupBuilder anEcsServiceSetup(String name) {
      return new EcsServiceSetupBuilder(name);
    }

    public EcsServiceSetupBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public EcsServiceSetupBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public EcsServiceSetupBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public EcsServiceSetupBuilder withLoadBalancerSettingId(String loadBalancerSettingId) {
      this.loadBalancerSettingId = loadBalancerSettingId;
      return this;
    }

    public EcsServiceSetupBuilder withAwsClusterService(AwsClusterService awsClusterService) {
      this.awsClusterService = awsClusterService;
      return this;
    }

    public EcsServiceSetupBuilder withSettingsService(SettingsService settingsService) {
      this.settingsService = settingsService;
      return this;
    }

    public EcsServiceSetupBuilder withServiceResourceService(ServiceResourceService serviceResourceService) {
      this.serviceResourceService = serviceResourceService;
      return this;
    }

    public EcsServiceSetupBuilder withInfrastructureMappingService(
        InfrastructureMappingService infrastructureMappingService) {
      this.infrastructureMappingService = infrastructureMappingService;
      return this;
    }

    public EcsServiceSetupBuilder withArtifactStreamService(ArtifactStreamService artifactStreamService) {
      this.artifactStreamService = artifactStreamService;
      return this;
    }

    public EcsServiceSetup build() {
      EcsServiceSetup ecsServiceSetup = new EcsServiceSetup(name);
      ecsServiceSetup.setId(id);
      ecsServiceSetup.setRequiredContextElementType(requiredContextElementType);
      ecsServiceSetup.setRollback(rollback);
      ecsServiceSetup.setLoadBalancerSettingId(loadBalancerSettingId);
      ecsServiceSetup.settingsService = this.settingsService;
      ecsServiceSetup.artifactStreamService = this.artifactStreamService;
      ecsServiceSetup.serviceResourceService = this.serviceResourceService;
      ecsServiceSetup.awsClusterService = this.awsClusterService;
      ecsServiceSetup.infrastructureMappingService = this.infrastructureMappingService;
      return ecsServiceSetup;
    }
  }
}
