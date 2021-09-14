/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.data.structure.EmptyPredicate;
import io.harness.k8s.model.HelmVersion;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HelmCommandFlagsUtils {
  public String applyHelmCommandFlags(
      String command, String commandType, Map<HelmSubCommandType, String> commandFlags, HelmVersion helmVersion) {
    String flags = "";
    if (isNotEmpty(commandFlags)) {
      HelmSubCommandType subCommandType = HelmSubCommandType.getSubCommandType(commandType, helmVersion);
      flags = commandFlags.getOrDefault(subCommandType, "");
      if (EmptyPredicate.isEmpty(flags)) {
        flags = "";
      }
    }

    return command.replace(HelmConstants.HELM_COMMAND_FLAG_PLACEHOLDER, flags);
  }
}
