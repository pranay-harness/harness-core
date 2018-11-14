package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;

import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.sm.ExecutionContext;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;

import java.util.List;

@Singleton
public class EcsStateHelper {
  public ContainerSetupParams buildContainerSetupParams(
      ExecutionContext context, EcsSetupStateConfig ecsSetupStateConfig) {
    Application app = ecsSetupStateConfig.getApp();
    Environment env = ecsSetupStateConfig.getEnv();
    ContainerTask containerTask = ecsSetupStateConfig.getContainerTask();

    String taskFamily = isNotBlank(ecsSetupStateConfig.getEcsServiceName())
        ? Misc.normalizeExpression(context.renderExpression(ecsSetupStateConfig.getEcsServiceName()))
        : EcsConvention.getTaskFamily(app.getName(), ecsSetupStateConfig.getServiceName(), env.getName());

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

    int serviceSteadyStateTimeout = ecsSetupStateConfig.getServiceSteadyStateTimeout();

    EcsInfrastructureMapping ecsInfrastructureMapping =
        (EcsInfrastructureMapping) ecsSetupStateConfig.getInfrastructureMapping();

    return anEcsSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(ecsSetupStateConfig.getServiceName())
        .withClusterName(ecsSetupStateConfig.getClusterName())
        .withImageDetails(ecsSetupStateConfig.getImageDetails())
        .withContainerTask(containerTask)
        .withLoadBalancerName(context.renderExpression(ecsSetupStateConfig.getLoadBalancerName()))
        .withInfraMappingId(ecsSetupStateConfig.getInfrastructureMapping().getUuid())
        .withRoleArn(context.renderExpression(ecsSetupStateConfig.getRoleArn()))
        .withTargetGroupArn(context.renderExpression(ecsSetupStateConfig.getTargetGroupArn()))
        .withTaskFamily(taskFamily)
        .withUseLoadBalancer(ecsSetupStateConfig.isUseLoadBalancer())
        .withRegion(ecsInfrastructureMapping.getRegion())
        .withVpcId(ecsInfrastructureMapping.getVpcId())
        .withSubnetIds(getArrayFromList(ecsInfrastructureMapping.getSubnetIds()))
        .withSecurityGroupIds(getArrayFromList(ecsInfrastructureMapping.getSecurityGroupIds()))
        .withAssignPublicIps(ecsInfrastructureMapping.isAssignPublicIp())
        .withExecutionRoleArn(ecsInfrastructureMapping.getExecutionRole())
        .withLaunchType(ecsInfrastructureMapping.getLaunchType())
        .withTargetContainerName(context.renderExpression(ecsSetupStateConfig.getTargetContainerName()))
        .withTargetPort(context.renderExpression(ecsSetupStateConfig.getTargetPort()))
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withRollback(ecsSetupStateConfig.isRollback())
        .withPreviousEcsServiceSnapshotJson(ecsSetupStateConfig.getPreviousEcsServiceSnapshotJson())
        .withEcsServiceSpecification(
            getServiceSpecWithRenderedExpression(ecsSetupStateConfig.getEcsServiceSpecification(), context))
        .withEcsServiceArn(ecsSetupStateConfig.getEcsServiceArn())
        .withIsDaemonSchedulingStrategy(ecsSetupStateConfig.isDaemonSchedulingStrategy())
        .withNewAwsAutoScalarConfigList(
            getNewAwsAutoScalarConfigListWithRenderedExpression(ecsSetupStateConfig.getAwsAutoScalarConfigs(), context))
        .build();
  }

  private EcsServiceSpecification getServiceSpecWithRenderedExpression(
      EcsServiceSpecification ecsServiceSpecification, ExecutionContext context) {
    if (ecsServiceSpecification == null || StringUtils.isBlank(ecsServiceSpecification.getServiceSpecJson())) {
      return ecsServiceSpecification;
    }

    ecsServiceSpecification.setServiceSpecJson(context.renderExpression(ecsServiceSpecification.getServiceSpecJson()));
    return ecsServiceSpecification;
  }

  private List<AwsAutoScalarConfig> getNewAwsAutoScalarConfigListWithRenderedExpression(
      List<AwsAutoScalarConfig> awsAutoScalarConfigs, ExecutionContext context) {
    if (isEmpty(awsAutoScalarConfigs)) {
      return awsAutoScalarConfigs;
    }

    awsAutoScalarConfigs.forEach(awsAutoScalarConfig -> {
      if (isNotBlank(awsAutoScalarConfig.getScalableTargetJson())) {
        awsAutoScalarConfig.setScalableTargetJson(
            context.renderExpression(awsAutoScalarConfig.getScalableTargetJson()));
      }

      if (isNotBlank(awsAutoScalarConfig.getScalingPolicyForTarget())) {
        awsAutoScalarConfig.setScalingPolicyForTarget(
            context.renderExpression(awsAutoScalarConfig.getScalingPolicyForTarget()));
        // Field " String ScalingPolicyForTarget", is only used when UI sends policyJson.
        // actual field used in all delegate tasks is "String[] ScalingPolicyJson". So we initialize this field using
        // vaoue set by UI.
        awsAutoScalarConfig.setScalingPolicyJson(new String[] {awsAutoScalarConfig.getScalingPolicyForTarget()});
      }
    });

    return awsAutoScalarConfigs;
  }

  private String[] getArrayFromList(List<String> input) {
    if (CollectionUtils.isEmpty(input)) {
      return new String[0];
    } else {
      return input.toArray(new String[0]);
    }
  }
}
