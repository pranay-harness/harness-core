package software.wings.beans.command;

import static java.util.stream.Collectors.toList;

import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by brett on 11/18/17
 */
public abstract class ContainerSetupCommandUnit extends AbstractCommandUnit {
  @Inject @Transient private transient DelegateLogService logService;

  static List<String[]> integerMapToListOfStringArray(Map<String, Integer> integerMap) {
    return integerMap.entrySet()
        .stream()
        .map(entry -> new String[] {entry.getKey(), entry.getValue().toString()})
        .collect(toList());
  }

  public ContainerSetupCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
    setArtifactNeeded(true);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    List<EncryptedDataDetail> cloudProviderCredentials = context.getCloudProviderCredentials();
    ContainerSetupParams setupParams = context.getContainerSetupParams();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(
        logService, context.getAccountId(), context.getAppId(), context.getActivityId(), getName());

    return executeInternal(context, cloudProviderSetting, cloudProviderCredentials, setupParams,
        context.getServiceVariables(), context.getSafeDisplayServiceVariables(), executionLogCallback);
  }

  protected abstract CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      ContainerSetupParams setupParams, Map<String, String> serviceVariables,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback);
}
