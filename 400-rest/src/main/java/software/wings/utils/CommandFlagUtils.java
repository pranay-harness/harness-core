package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.HelmCommandFlagConstants.getHelmSubCommands;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidRequestException;
import io.harness.helm.HelmConstants;
import io.harness.k8s.model.HelmVersion;

import software.wings.beans.HelmCommandFlag;
import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import com.google.inject.Singleton;
import java.util.Set;
import lombok.experimental.UtilityClass;

@Singleton
@UtilityClass
public class CommandFlagUtils {
  public static HelmSubCommand getHelmSubCommand(HelmVersion version, String commandType) {
    Set<HelmSubCommand> subCommands = getHelmSubCommands(version);
    for (HelmSubCommand subCommand : subCommands) {
      Set<String> commandTypes = subCommand.getCommandTypes();
      if (commandTypes.contains(commandType)) {
        return subCommand;
      }
    }
    return null;
  }

  public String applyHelmCommandFlags(
      String command, HelmCommandFlag commandFlag, String commandType, HelmVersion helmVersion) {
    String flags;
    if (null == commandFlag) {
      flags = "";
    } else {
      HelmSubCommand subCommand = getHelmSubCommand(helmVersion, commandType);
      String value = isEmpty(commandFlag.getValueMap()) ? null : commandFlag.getValueMap().get(subCommand);
      flags = isBlank(value) ? "" : value;
    }
    return command.replace(HelmConstants.HELM_COMMAND_FLAG_PLACEHOLDER, flags);
  }

  public static void validateHelmCommandFlags(HelmCommandFlag helmCommandFlag, HelmVersion helmVersion) {
    if (null != helmCommandFlag) {
      Set<HelmSubCommand> helmSubCommands = getHelmSubCommands(helmVersion);

      helmCommandFlag.getValueMap().forEach((k, v) -> {
        if (!helmSubCommands.contains(k)) {
          throw new InvalidRequestException(String.format("Invalid subCommand [%s] provided", k), USER);
        }
        if (isEmpty(v)) {
          throw new InvalidRequestException("Command flag provided is null", USER);
        }
      });
    }
  }
}
