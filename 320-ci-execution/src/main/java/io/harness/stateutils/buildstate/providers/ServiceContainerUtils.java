/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ID_PREFIX;
import static io.harness.common.CIExecutionConstants.IMAGE_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.SERVICE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.STEP_COMMAND;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.CI)
public class ServiceContainerUtils {
  public static List<String> getCommand() {
    List<String> command = new ArrayList<>();
    command.add(STEP_COMMAND);
    return command;
  }

  public static List<String> getArguments(String serviceID, String image, Integer port) {
    List<String> args = new ArrayList<>();
    args.add(SERVICE_ARG_COMMAND);
    args.add(ID_PREFIX);
    args.add(serviceID);
    args.add(IMAGE_PREFIX);
    args.add(image);

    args.add(PORT_PREFIX);
    args.add(port.toString());
    return args;
  }
}
