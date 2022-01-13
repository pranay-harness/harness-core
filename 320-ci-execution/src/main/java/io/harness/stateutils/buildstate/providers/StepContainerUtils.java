/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.STEP_COMMAND;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.CI)
public class StepContainerUtils {
  public static List<String> getCommand() {
    List<String> command = new ArrayList<>();
    command.add(STEP_COMMAND);
    return command;
  }

  public static List<String> getArguments(Integer port) {
    List<String> args = new ArrayList<>();
    args.add(PORT_PREFIX);
    args.add(port.toString());
    return args;
  }
}
