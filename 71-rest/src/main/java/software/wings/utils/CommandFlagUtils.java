package software.wings.utils;

import static software.wings.beans.HelmCommandFlagConstants.getHelmSubCommands;

import io.harness.k8s.model.HelmVersion;

import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import com.google.inject.Singleton;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@Singleton
@UtilityClass
public class CommandFlagUtils {
  public static HelmSubCommand getHelmSubCommand(@NotNull HelmVersion version, String commandType) {
    Set<HelmSubCommand> subCommands = getHelmSubCommands(version);
    for (HelmSubCommand subCommand : subCommands) {
      Set<String> commandTypes = subCommand.getCommandTypes();
      if (commandTypes.contains(commandType)) {
        return subCommand;
      }
    }
    return null;
  }
}
