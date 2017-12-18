package software.wings.beans.command;

import com.google.inject.Inject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 11/18/17
 */
public abstract class ContainerSetupCommandUnit extends AbstractCommandUnit {
  static final int KEEP_N_REVISIONS = 3;

  @Inject @Transient private transient DelegateLogService logService;

  public ContainerSetupCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    List<EncryptedDataDetail> cloudProviderCredentials = context.getCloudProviderCredentials();
    ContainerSetupParams setupParams = context.getContainerSetupParams();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);

    try {
      context.setCommandExecutionData(
          ContainerSetupCommandUnitExecutionData.builder()
              .containerServiceName(executeInternal(cloudProviderSetting, cloudProviderCredentials, setupParams,
                  context.getServiceVariables(), executionLogCallback))
              .build());
      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(ex.getMessage(), LogLevel.ERROR);
      if (ex instanceof WingsException) {
        throw ex;
      }
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, ex.getMessage());
    }
  }

  protected abstract String executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerSetupParams setupParams,
      Map<String, String> serviceVariables, ExecutionLogCallback executionLogCallback);

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static abstract class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml(String commandUnitType) {
      super(commandUnitType);
    }

    public Yaml(String name, String commandUnitType, String deploymentType) {
      super(name, commandUnitType, deploymentType);
    }
  }
}
