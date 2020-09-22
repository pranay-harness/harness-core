package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.INPUT_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_COMMAND;
import static io.harness.common.CIExecutionConstants.LOG_PATH;
import static io.harness.common.CIExecutionConstants.LOG_PATH_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.PORTS_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.SERVER_PREFIX;
import static io.harness.common.CIExecutionConstants.STAGE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.TMP_PATH;
import static io.harness.common.CIExecutionConstants.TMP_PATH_ARG_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LiteEngineTaskUtils {
  public List<String> getLiteEngineCommand() {
    List<String> command = new ArrayList<>();
    command.add(LITE_ENGINE_COMMAND);
    return command;
  }

  public List<String> getMainLiteEngineArguments(String serializedLiteEngineTaskStepInfo, List<String> ports) {
    List<String> args = new ArrayList<>();
    args.add(STAGE_ARG_COMMAND);
    args.add(INPUT_ARG_PREFIX);
    args.add(serializedLiteEngineTaskStepInfo);
    args.add(LOG_PATH_ARG_PREFIX);
    args.add(LOG_PATH);
    args.add(TMP_PATH_ARG_PREFIX);
    args.add(TMP_PATH);
    if (isNotEmpty(ports)) {
      args.add(PORTS_PREFIX);
      args.addAll(ports);
    }
    return args;
  }

  public List<String> getWorkerLiteEngineArguments(String port) {
    List<String> args = new ArrayList<>();
    args.add(SERVER_PREFIX);
    args.add(PORT_PREFIX);
    args.add(port);
    args.add(LOG_PATH_ARG_PREFIX);
    args.add(LOG_PATH);
    args.add(TMP_PATH_ARG_PREFIX);
    args.add(TMP_PATH);
    return args;
  }
}
