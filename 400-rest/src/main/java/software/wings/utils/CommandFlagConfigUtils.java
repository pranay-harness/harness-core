/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.HelmCommandFlagConstants.getHelmSubCommands;

import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.HelmVersion;

import software.wings.beans.HelmCommandFlagConfig;
import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import com.google.inject.Singleton;
import java.util.Set;
import lombok.experimental.UtilityClass;

@Singleton
@UtilityClass
public class CommandFlagConfigUtils {
  public void validateHelmCommandFlags(HelmCommandFlagConfig helmCommandFlag, HelmVersion helmVersion) {
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
