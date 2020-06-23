package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_COMMAND;
import static io.harness.common.CIExecutionConstants.LOGPATH_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.LOG_PATH;
import static io.harness.common.CIExecutionConstants.STAGE_ARG_PREFIX;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;

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
    args.add(STAGE_ARG_PREFIX);
    args.add(protobufSerializer.serialize(liteEngineTaskStepInfo.getEnvSetup().getSteps()));
    args.add(LOGPATH_ARG_PREFIX);
    args.add(MOUNT_PATH + LOG_PATH);
    return args;
  }
}
