package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.STEP_COMMAND;

import java.util.ArrayList;
import java.util.List;

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
