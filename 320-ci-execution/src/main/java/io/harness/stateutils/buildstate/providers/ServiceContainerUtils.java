package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ARGS_PREFIX;
import static io.harness.common.CIExecutionConstants.ENTRYPOINT_PREFIX;
import static io.harness.common.CIExecutionConstants.ID_PREFIX;
import static io.harness.common.CIExecutionConstants.IMAGE_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.SERVICE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.STEP_COMMAND;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.ArrayList;
import java.util.List;

public class ServiceContainerUtils {
  public static List<String> getCommand() {
    List<String> command = new ArrayList<>();
    command.add(STEP_COMMAND);
    return command;
  }

  public static List<String> getArguments(
      String serviceID, String image, List<String> entrypoint, List<String> imageArgs, Integer port) {
    List<String> args = new ArrayList<>();
    args.add(SERVICE_ARG_COMMAND);
    args.add(ID_PREFIX);
    args.add(serviceID);
    args.add(IMAGE_PREFIX);
    args.add(image);

    if (!isEmpty(entrypoint)) {
      args.add(ENTRYPOINT_PREFIX);
      args.addAll(entrypoint);
    }

    if (!isEmpty(imageArgs)) {
      args.add(ARGS_PREFIX);
      args.addAll(imageArgs);
    }

    args.add(PORT_PREFIX);
    args.add(port.toString());
    return args;
  }
}
