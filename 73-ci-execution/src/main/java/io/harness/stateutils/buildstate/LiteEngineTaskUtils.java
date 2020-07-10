package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.INPUT_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_COMMAND;
import static io.harness.common.CIExecutionConstants.LOG_PATH;
import static io.harness.common.CIExecutionConstants.LOG_PATH_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.STAGE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.TMP_PATH;
import static io.harness.common.CIExecutionConstants.TMP_PATH_ARG_PREFIX;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.seriazlier.ProtobufSerializer;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.yaml.core.Execution;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class LiteEngineTaskUtils {
  @Inject ProtobufSerializer<Execution> protobufSerializer;

  public List<String> getLiteEngineCommand() {
    List<String> command = new ArrayList<>();
    command.add(LITE_ENGINE_COMMAND);
    return command;
  }

  public List<String> getLiteEngineArguments(LiteEngineTaskStepInfo liteEngineTaskStepInfo) {
    List<String> args = new ArrayList<>();
    args.add(STAGE_ARG_COMMAND);
    args.add(INPUT_ARG_PREFIX);
    args.add(protobufSerializer.serialize(liteEngineTaskStepInfo.getEnvSetup().getSteps()));
    args.add(LOG_PATH_ARG_PREFIX);
    args.add(LOG_PATH);
    args.add(TMP_PATH_ARG_PREFIX);
    args.add(TMP_PATH);
    return args;
  }
}
