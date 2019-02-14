package software.wings.beans.command;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenRoute53SetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsBlueGreenSetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsSetupCommandHandler;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53ServiceSetupRequest;
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
  @Inject @Transient private transient EcsBlueGreenRoute53SetupCommandHandler ecsBlueGreenRoute53SetupCommandHandler;
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
        if (ecsSetupParams.isUseRoute53DNSSwap()) {
          return handleEcsBlueGreenServiceSetupRoute53(cloudProviderSetting, encryptedDataDetails,
              (EcsSetupParams) containerSetupParams, serviceVariables, safeDisplayServiceVariables,
              commandExecutionDataBuilder, executionLogCallback);
        } else {
          return handleEcsBlueGreenServiceSetup(cloudProviderSetting, encryptedDataDetails,
              (EcsSetupParams) containerSetupParams, serviceVariables, safeDisplayServiceVariables,
              commandExecutionDataBuilder, executionLogCallback);
        }
      } else {
        return handleEcsNonBlueGreenServiceSetup(cloudProviderSetting, encryptedDataDetails,
            (EcsSetupParams) containerSetupParams, serviceVariables, safeDisplayServiceVariables,
            commandExecutionDataBuilder, executionLogCallback);
      }
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getMessage(ex), ex);
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
  private CommandExecutionStatus handleEcsBlueGreenServiceSetupRoute53(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, EcsSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder,
      ExecutionLogCallback executionLogCallback) {
    ecsBlueGreenRoute53SetupCommandHandler.executeTask(EcsBGRoute53ServiceSetupRequest.builder()
                                                           .ecsSetupParams(containerSetupParams)
                                                           .awsConfig((AwsConfig) cloudProviderSetting.getValue())
                                                           .clusterName(containerSetupParams.getClusterName())
                                                           .region(containerSetupParams.getRegion())
                                                           .safeDisplayServiceVariables(safeDisplayServiceVariables)
                                                           .serviceVariables(serviceVariables)
                                                           .commandUnitExecutionDataBuilder(commandExecutionDataBuilder)
                                                           .executionLogCallback(executionLogCallback)
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
                                                    .build(),
        encryptedDataDetails);
    return CommandExecutionStatus.SUCCESS;
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
