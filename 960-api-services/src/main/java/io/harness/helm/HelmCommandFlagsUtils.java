package io.harness.helm;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.k8s.model.HelmVersion;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HelmCommandFlagsUtils {
  public String applyHelmCommandFlags(
      String command, String commandType, Map<HelmSubCommandType, String> commandFlags, HelmVersion helmVersion) {
    String flags = "";
    if (hasSome(commandFlags)) {
      HelmSubCommandType subCommandType = HelmSubCommandType.getSubCommandType(commandType, helmVersion);
      flags = commandFlags.getOrDefault(subCommandType, "");
    }

    return command.replace(HelmConstants.HELM_COMMAND_FLAG_PLACEHOLDER, flags);
  }
}
