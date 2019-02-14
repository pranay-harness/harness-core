package software.wings.sm.states;

import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult;
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
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.sm.ExecutionContext;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsBlueGreenServiceSetup extends ContainerServiceSetup {
  private static final Logger logger = LoggerFactory.getLogger(EcsBlueGreenServiceSetup.class);

  private String ecsServiceName;
  private boolean useLoadBalancer;
  private String loadBalancerName;
  @Attributes(title = "Stage TargetGroup Arn", required = false) private String targetGroupArn;
  private String roleArn;
  @Attributes(title = "Prod Listener ARN", required = true) private String prodListenerArn;
  @Attributes(title = "Stage Listener ARN", required = true) private String stageListenerArn;
  private String targetContainerName;
  private String targetPort;
  @Attributes(title = "Stage Listener Port", required = false) private String stageListenerPort;
  private String commandName = "Setup Service Cluster";
  @Inject private transient EcsStateHelper ecsStateHelper;

  public EcsBlueGreenServiceSetup(String name) {
    super(name, ECS_BG_SERVICE_SETUP.name());
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName) {
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    EcsServiceSpecification serviceSpecification =
        serviceResourceService.getEcsServiceSpecification(app.getUuid(), service.getUuid());
    return ecsStateHelper.buildContainerSetupParams(context,
        EcsSetupStateConfig.builder()
            .app(app)
            .env(env)
            .service(service)
            .infrastructureMapping(infrastructureMapping)
            .clusterName(clusterName)
            .containerTask(containerTask)
            .ecsServiceName(ecsServiceName)
            .imageDetails(imageDetails)
            .loadBalancerName(loadBalancerName)
            .roleArn(roleArn)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .targetContainerName(targetContainerName)
            .stageListenerArn(stageListenerArn)
            .prodListenerArn(prodListenerArn)
            .stageListenerArn(stageListenerArn)
            .stageListenerPort(stageListenerPort)
            .blueGreen(true)
            .targetPort(targetPort)
            .useLoadBalancer(true)
            .serviceName(serviceName)
            .ecsServiceSpecification(serviceSpecification)
            .isDaemonSchedulingStrategy(false)
            .targetGroupArn(targetGroupArn)
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
  protected String getDeploymentType() {
    return DeploymentType.ECS.name();
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof EcsInfrastructureMapping;
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    return super.getClusterNameFromContextElement(context).split("/")[1];
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setTargetGroupArn(String stageTargetGroupArn) {
    this.targetGroupArn = stageTargetGroupArn;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public String getProdListenerArn() {
    return prodListenerArn;
  }

  public void setProdListenerArn(String prodListenerArn) {
    this.prodListenerArn = prodListenerArn;
  }

  public String getStageListenerArn() {
    return stageListenerArn;
  }

  public void setStageListenerArn(String stageListenerArn) {
    this.stageListenerArn = stageListenerArn;
  }

  public String getStageListenerPort() {
    return stageListenerPort;
  }

  public void setStageListenerPort(String stageListenerPort) {
    this.stageListenerPort = stageListenerPort;
  }

  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  public String getRoleArn() {
    return roleArn;
  }

  public void setTargetContainerName(String targetContainerName) {
    this.targetContainerName = targetContainerName;
  }

  public String getTargetContainerName() {
    return targetContainerName;
  }

  public boolean isUseLoadBalancer() {
    return useLoadBalancer;
  }

  public void setUseLoadBalancer(boolean useLoadBalancer) {
    this.useLoadBalancer = useLoadBalancer;
  }

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
  }

  public String getTargetPort() {
    return targetPort;
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = targetPort;
  }
}
