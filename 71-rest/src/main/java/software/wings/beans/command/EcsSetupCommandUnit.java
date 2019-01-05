package software.wings.beans.command;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenSetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsSetupCommandHandler;
import software.wings.helpers.ext.ecs.request.EcsBGServiceSetupRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType;
import software.wings.helpers.ext.ecs.request.EcsServiceSetupRequest;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 11/18/17
 */
public class EcsSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(EcsSetupCommandUnit.class);
  @Inject @Transient private transient EcsSetupCommandHandler ecsSetupCommandHandler;
  @Inject @Transient private transient EcsBlueGreenSetupCommandHandler ecsBlueGreenSetupCommandHandler;
  public static final String ERROR = "Error: ";

  public EcsSetupCommandUnit() {
    super(CommandUnitType.ECS_SETUP);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupParams containerSetupParams, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
        ContainerSetupCommandUnitExecutionData.builder();
    try {
      EcsSetupParams ecsSetupParams = (EcsSetupParams) containerSetupParams;
      if (ecsSetupParams.isBlueGreen()) {
        return handleEcsBlueGreenServiceSetup(cloudProviderSetting, encryptedDataDetails,
            (EcsSetupParams) containerSetupParams, serviceVariables, safeDisplayServiceVariables,
            commandExecutionDataBuilder, executionLogCallback);
      } else {
        return handleEcsNonBlueGreenServiceSetup(cloudProviderSetting, encryptedDataDetails,
            (EcsSetupParams) containerSetupParams, serviceVariables, safeDisplayServiceVariables,
            commandExecutionDataBuilder, executionLogCallback);
      }
    } catch (Exception ex) {
      logger.error(Misc.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  @NotNull
  private CommandExecutionStatus handleEcsNonBlueGreenServiceSetup(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    ecsSetupCommandHandler.executeTask(EcsServiceSetupRequest.builder()
                                           .ecsSetupParams(containerSetupParams)
                                           .awsConfig((AwsConfig) cloudProviderSetting.getValue())
                                           .clusterName(containerSetupParams.getClusterName())
                                           .region(containerSetupParams.getRegion())
                                           .safeDisplayServiceVariables(safeDisplayServiceVariables)
                                           .serviceVariables(serviceVariables)
                                           .commandUnitExecutionDataBuilder(commandExecutionDataBuilder)
                                           .executionLogCallback(executionLogCallback)
                                           .ecsCommandType(EcsCommandType.SERVICE_SETUP)
                                           .build(),
        encryptedDataDetails);

    return CommandExecutionStatus.SUCCESS;
  }

  @NotNull
  private CommandExecutionStatus handleEcsBlueGreenServiceSetup(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    ecsBlueGreenSetupCommandHandler.executeTask(EcsBGServiceSetupRequest.builder()
                                                    .ecsSetupParams(containerSetupParams)
                                                    .awsConfig((AwsConfig) cloudProviderSetting.getValue())
                                                    .clusterName(containerSetupParams.getClusterName())
                                                    .region(containerSetupParams.getRegion())
                                                    .safeDisplayServiceVariables(safeDisplayServiceVariables)
                                                    .serviceVariables(serviceVariables)
                                                    .commandUnitExecutionDataBuilder(commandExecutionDataBuilder)
                                                    .executionLogCallback(executionLogCallback)
                                                    .ecsCommandType(EcsCommandType.BG_SERVICE_SETUP)
                                                    .build(),
        encryptedDataDetails);
    return CommandExecutionStatus.SUCCESS;
  }

  private void logLoadBalancerInfo(ExecutionLogCallback executionLogCallback, EcsSetupParams setupParams) {
    if (setupParams.isUseLoadBalancer()) {
      executionLogCallback.saveExecutionLog("Load Balancer Name: " + setupParams.getLoadBalancerName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Target Group ARN: " + setupParams.getTargetGroupArn(), LogLevel.INFO);
      if (isNotBlank(setupParams.getRoleArn())) {
        executionLogCallback.saveExecutionLog("Role ARN: " + setupParams.getRoleArn(), LogLevel.INFO);
      }
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ECS_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.ECS_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.ECS_SETUP.name(), deploymentType);
    }
  }
}
