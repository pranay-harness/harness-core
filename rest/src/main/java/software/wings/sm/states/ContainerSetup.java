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
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ClusterService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.service.impl.JenkinsSettingProvider;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.stencils.EnumData;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ContainerSetup extends State {
  @EnumData(enumDataProvider = AwsSettingProvider.class) @Attributes(title = "Cloud Config") private String settingId;

  // should come from service
  private String clusterName;

  // should come from service
  private String serviceName;

  // should come from service variables
  private String loadBalancerName;

  // should come from service variables
  private String imageName;

  @Inject @Transient private transient ClusterService clusterService;

  @Inject @Transient private transient SettingsService settingsService;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public ContainerSetup(String name) {
    super(name, CONTAINER_SETUP.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    SettingAttribute settingAttribute = settingsService.get(settingId);

    TaskDefinition taskDefinition = clusterService.createTask(settingAttribute,
        new RegisterTaskDefinitionRequest()
            .withContainerDefinitions(
                new ContainerDefinition()
                    .withName("containerName")
                    .withImage(imageName)
                    .withPortMappings(new PortMapping().withContainerPort(8080).withProtocol(TransportProtocol.Tcp))
                    .withEnvironment()
                    .withMemoryReservation(128))
            .withFamily(appName + serviceName + envName));

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

  /**
   * Getter for property 'settingId'.
   *
   * @return Value for property 'settingId'.
   */
  public String getSettingId() {
    return settingId;
  }

  /**
   * Setter for property 'settingId'.
   *
   * @param settingId Value to set for property 'settingId'.
   */
  public void setSettingId(String settingId) {
    this.settingId = settingId;
  }

  /**
   * Getter for property 'clusterName'.
   *
   * @return Value for property 'clusterName'.
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Setter for property 'clusterName'.
   *
   * @param clusterName Value to set for property 'clusterName'.
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Getter for property 'serviceName'.
   *
   * @return Value for property 'serviceName'.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Setter for property 'serviceName'.
   *
   * @param serviceName Value to set for property 'serviceName'.
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Getter for property 'loadBalancerName'.
   *
   * @return Value for property 'loadBalancerName'.
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Setter for property 'loadBalancerName'.
   *
   * @param loadBalancerName Value to set for property 'loadBalancerName'.
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'imageName'.
   *
   * @return Value for property 'imageName'.
   */
  public String getImageName() {
    return imageName;
  }

  /**
   * Setter for property 'imageName'.
   *
   * @param imageName Value to set for property 'imageName'.
   */
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }
}
