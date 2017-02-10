package software.wings.sm.states;

import static software.wings.api.EcsServiceElement.EcsServiceElementBuilder.anEcsServiceElement;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.container.EcsContainerTask;
import software.wings.cloudprovider.ClusterService;
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
import software.wings.stencils.Expand;
import software.wings.utils.ECSConvention;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class EcsServiceSetup extends State {
  @Attributes(title = "Load Balancer")
  @Expand(dataProvider = LoadBalancerDataProvider.class)
  @EnumData(enumDataProvider = LoadBalancerDataProvider.class)
  private String loadBalancerSettingId;

  @Inject @Transient private transient ClusterService clusterService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private ArtifactStreamService artifactStreamService;

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
    String computeProviderId = phaseElement.getComputeProviderId();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
    String imageName = fetchArtifactImageName(artifact);

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    String clusterName = infrastructureMappingService.getClusterName(
        app.getUuid(), serviceId, env.getUuid()); // TODO:: remove this call with infrMapping get call

    ElasticLoadBalancerConfig elasticLoadBalancerConfig =
        (ElasticLoadBalancerConfig) settingsService.get(loadBalancerSettingId).getValue();
    String loadBalancerName = elasticLoadBalancerConfig.getLoadBalancerName();

    Service service = serviceResourceService.get(app.getAppId(), serviceId);
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);

    EcsContainerTask ecsContainerTask = (EcsContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
        app.getAppId(), serviceId, DeploymentType.ECS.name());

    List<ContainerDefinition> containerDefinitions =
        ecsContainerTask.getContainerDefinitions()
            .stream()
            .map(containerDefinition -> createContainerDefinition(imageName, containerDefinition))
            .collect(Collectors.toList());

    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(containerDefinitions)
            .withFamily(ECSConvention.getTaskFamily(app.getName(), service.getName(), env.getName()));

    TaskDefinition taskDefinition = clusterService.createTask(computeProviderSetting, registerTaskDefinitionRequest);

    clusterService.createService(computeProviderSetting,
        new CreateServiceRequest()
            .withServiceName(ECSConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()))
            .withCluster(clusterName)
            .withDesiredCount(0)
            .withDeploymentConfiguration(
                new DeploymentConfiguration().withMaximumPercent(200).withMinimumHealthyPercent(100))
            .withTaskDefinition(taskDefinition.getFamily() + ":" + taskDefinition.getRevision())
            .withLoadBalancers(
                new LoadBalancer()
                    .withLoadBalancerName(loadBalancerName)
                    .withTargetGroupArn(
                        "arn:aws:elasticloadbalancing:us-east-1:830767422336:targetgroup/testecsgroup/5840102c72984871")
                    .withContainerName("containerName")
                    .withContainerPort(8080)));

    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .addElement(
            anEcsServiceElement().withUuid(serviceId).withName(service.getName()).withClusterName(clusterName).build())
        .build();
  }

  public ContainerDefinition createContainerDefinition(
      String imageName, EcsContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerDefinition containerDefinition =
        new ContainerDefinition().withName(wingsContainerDefinition.getName()).withImage(imageName);

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

  public String fetchArtifactImageName(Artifact artifact) {
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    if (!(artifactStream instanceof DockerArtifactStream)) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message",
          artifactStream.getArtifactStreamType() + " artifact source can't be used for Containers");
    }

    DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;

    return dockerArtifactStream.getImageName();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getLoadBalancerSettingId() {
    return loadBalancerSettingId;
  }

  public void setLoadBalancerSettingId(String loadBalancerSettingId) {
    this.loadBalancerSettingId = loadBalancerSettingId;
  }
}
