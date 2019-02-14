package software.wings.sm.states;

import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.sm.ExecutionContext;

import java.util.List;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsServiceSetup extends ContainerServiceSetup {
  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/ECSLoadBalancerModal.js

  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsServiceSetup.class);

  private String ecsServiceName;
  private boolean useLoadBalancer;
  private String loadBalancerName;
  private String targetGroupArn;
  private String roleArn;
  private String targetContainerName;
  private String targetPort;
  private String commandName = "Setup Service Cluster";
  private List<AwsAutoScalarConfig> awsAutoScalarConfigs;
  @Inject EcsStateHelper ecsStateHelper;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public EcsServiceSetup(String name) {
    super(name, ECS_SERVICE_SETUP.name());
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName) {
    EcsServiceSpecification serviceSpecification =
        serviceResourceService.getEcsServiceSpecification(app.getUuid(), service.getUuid());

    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    return ecsStateHelper.buildContainerSetupParams(context,
        EcsSetupStateConfig.builder()
            .service(service)
            .app(app)
            .env(env)
            .infrastructureMapping(infrastructureMapping)
            .clusterName(clusterName)
            .containerTask(containerTask)
            .roleArn(roleArn)
            .ecsServiceName(ecsServiceName)
            .imageDetails(imageDetails)
            .loadBalancerName(loadBalancerName)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .targetContainerName(targetContainerName)
            .serviceName(serviceName)
            .targetGroupArn(targetGroupArn)
            .targetPort(targetPort)
            .useLoadBalancer(useLoadBalancer)
            .ecsServiceSpecification(serviceSpecification)
            .isDaemonSchedulingStrategy(false)
            .awsAutoScalarConfigs(awsAutoScalarConfigs)
            .build());
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails) {
    return ecsStateHelper.buildContainerServiceElement(context, executionResult, status, imageDetails,
        getMaxInstances(), getFixedInstances(), getDesiredInstanceCount(), getResizeStrategy(),
        getServiceSteadyStateTimeout(), logger);
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof EcsInfrastructureMapping;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.ECS.name();
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    return super.getClusterNameFromContextElement(context).split("/")[1];
  }

  /**
   * Gets load balancer setting id.
   *
   * @return the load balancer setting id
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer setting id.
   *
   * @param loadBalancerName the load balancer setting id
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'targetGroupArn'.
   *
   * @return Value for property 'targetGroupArn'.
   */
  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  /**
   * Setter for property 'targetGroupArn'.
   *
   * @param targetGroupArn Value to set for property 'targetGroupArn'.
   */
  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  /**
   * Getter for property 'roleArn'.
   *
   * @return Value for property 'roleArn'.
   */
  public String getRoleArn() {
    return roleArn;
  }

  /**
   * Setter for property 'roleArn'.
   *
   * @param roleArn Value to set for property 'roleArn'.
   */
  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  /**
   * Getter for property 'useLoadBalancer'.
   *
   * @return Value for property 'useLoadBalancer'.
   */
  public boolean isUseLoadBalancer() {
    return useLoadBalancer;
  }

  /**
   * Setter for property 'useLoadBalancer'.
   *
   * @param useLoadBalancer Value to set for property 'useLoadBalancer'.
   */
  public void setUseLoadBalancer(boolean useLoadBalancer) {
    this.useLoadBalancer = useLoadBalancer;
  }

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
  }

  public String getTargetContainerName() {
    return targetContainerName;
  }

  public void setTargetContainerName(String targetContainerName) {
    this.targetContainerName = targetContainerName;
  }

  public String getTargetPort() {
    return targetPort;
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = targetPort;
  }

  public List<AwsAutoScalarConfig> getAwsAutoScalarConfigs() {
    return awsAutoScalarConfigs;
  }

  public void setAwsAutoScalarConfigs(List<AwsAutoScalarConfig> awsAutoScalarConfigs) {
    this.awsAutoScalarConfigs = awsAutoScalarConfigs;
  }
}
