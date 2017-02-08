package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.CONTAINER_SETUP;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.DeploymentConfiguration;
import com.amazonaws.services.ecs.model.LoadBalancer;
import com.amazonaws.services.ecs.model.PortMapping;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PhaseElement;
import software.wings.beans.Application;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.cloudprovider.ClusterService;
import software.wings.common.Constants;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ECSConvention;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ContainerSetup extends State {
  @Attributes(title = "Load Balancer") private String loadBalancerSettingId;

  @Inject @Transient private transient ClusterService clusterService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public ContainerSetup(String name) {
    super(name, CONTAINER_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String computeProviderId = phaseElement.getComputeProviderId();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
    // TODO - image names
    String imageName = null;

    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    // TODO - It should be pulled from the InfraMapping
    String clusterName = null;

    // TODO - elasticLoadBalancerConfig can pulled using settingsService for a given loadBalancerSettingId
    ElasticLoadBalancerConfig elasticLoadBalancerConfig = null;
    String loadBalancerName = elasticLoadBalancerConfig.getLoadBalancerName();

    Service service = serviceResourceService.get(app.getAppId(), serviceId);

    SettingAttribute settingAttribute = settingsService.get(computeProviderId);

    TaskDefinition taskDefinition = clusterService.createTask(settingAttribute,
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(
                new ContainerDefinition()
                    .withName("containerName")
                    .withImage(imageName)
                    .withPortMappings(new PortMapping().withContainerPort(8080).withProtocol(TransportProtocol.Tcp))
                    .withEnvironment()
                    .withMemoryReservation(128))
            .withFamily(ECSConvention.getTaskFamily(app.getName(), service.getName(), env.getName())));

    clusterService.createService(settingAttribute,
        new CreateServiceRequest()
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

    return anExecutionResponse().withExecutionStatus(ExecutionStatus.SUCCESS).build();
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
