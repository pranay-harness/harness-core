/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
