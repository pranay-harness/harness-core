package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    String taskFamily = isNotBlank(ecsServiceName)
        ? Misc.normalizeExpression(context.renderExpression(ecsServiceName))
        : EcsConvention.getTaskFamily(app.getName(), serviceName, env.getName());

    if (containerTask != null) {
      EcsContainerTask ecsContainerTask = (EcsContainerTask) containerTask;
      ecsContainerTask.getContainerDefinitions()
          .stream()
          .filter(containerDefinition -> isNotEmpty(containerDefinition.getCommands()))
          .forEach(containerDefinition
              -> containerDefinition.setCommands(
                  containerDefinition.getCommands().stream().map(context::renderExpression).collect(toList())));
      if (ecsContainerTask.getAdvancedConfig() != null) {
        ecsContainerTask.setAdvancedConfig(context.renderExpression(ecsContainerTask.getAdvancedConfig()));
      }
    }

    EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
    return anEcsSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(serviceName)
        .withClusterName(clusterName)
        .withImageDetails(imageDetails)
        .withContainerTask(containerTask)
        .withLoadBalancerName(loadBalancerName)
        .withInfraMappingId(infrastructureMapping.getUuid())
        .withRoleArn(roleArn)
        .withTargetGroupArn(targetGroupArn)
        .withTaskFamily(taskFamily)
        .withUseLoadBalancer(useLoadBalancer)
        .withRegion(ecsInfrastructureMapping.getRegion())
        .withVpcId(ecsInfrastructureMapping.getVpcId())
        .withSubnetIds(getSubnetArray(ecsInfrastructureMapping.getSubnetIds()))
        .withSecurityGroupIds(getSubnetArray(ecsInfrastructureMapping.getSecurityGroupIds()))
        .withAssignPublicIps(ecsInfrastructureMapping.isAssignPublicIp())
        .withExecutionRoleArn(ecsInfrastructureMapping.getExecutionRole())
        .withLaunchType(ecsInfrastructureMapping.getLaunchType())
        .withTargetContainerName(targetContainerName)
        .withTargetPort(targetPort)
        .build();
  }

  private String[] getSubnetArray(List<String> input) {
    if (CollectionUtils.isEmpty(input)) {
      return new String[0];
    } else {
      return input.toArray(new String[0]);
    }
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    EcsSetupParams setupParams = (EcsSetupParams) executionData.getContainerSetupParams();
    Integer maxVal = null;
    if (isNotBlank(getMaxInstances())) {
      try {
        maxVal = Integer.valueOf(context.renderExpression(getMaxInstances()));
      } catch (NumberFormatException e) {
        logger.error("Invalid number format for max instances: {}", context.renderExpression(getMaxInstances()), e);
      }
    }
    int evaluatedMaxInstances = maxVal != null ? maxVal : DEFAULT_MAX;
    int maxInstances = evaluatedMaxInstances == 0 ? DEFAULT_MAX : evaluatedMaxInstances;
    int evaluatedFixedInstances = isNotBlank(getFixedInstances())
        ? Integer.parseInt(context.renderExpression(getFixedInstances()))
        : maxInstances;
    int fixedInstances = evaluatedFixedInstances == 0 ? maxInstances : evaluatedFixedInstances;
    ResizeStrategy resizeStrategy = getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy();
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
    ContainerServiceElementBuilder containerServiceElementBuilder =
        ContainerServiceElement.builder()
            .uuid(executionData.getServiceId())
            .image(imageDetails.getName() + ":" + imageDetails.getTag())
            .useFixedInstances(FIXED_INSTANCES.equals(getDesiredInstanceCount()))
            .fixedInstances(fixedInstances)
            .maxInstances(maxInstances)
            .resizeStrategy(resizeStrategy)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .clusterName(executionData.getClusterName())
            .deploymentType(DeploymentType.ECS)
            .infraMappingId(setupParams.getInfraMappingId());
    if (executionResult != null) {
      ContainerSetupCommandUnitExecutionData setupExecutionData =
          (ContainerSetupCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (setupExecutionData != null) {
        containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName())
            .activeServiceCounts(setupExecutionData.getActiveServiceCounts());
        int totalActiveServiceCount = Optional.ofNullable(setupExecutionData.getActiveServiceCounts())
                                          .orElse(new ArrayList<>())
                                          .stream()
                                          .mapToInt(item -> Integer.valueOf(item[1]))
                                          .sum();
        if (totalActiveServiceCount > 0) {
          containerServiceElementBuilder.maxInstances(totalActiveServiceCount);
        }
      }
    }
    return containerServiceElementBuilder.build();
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
}
