package io.harness.states;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_STEP_COMMAND_FORMAT;
import static io.harness.common.CIExecutionConstants.LOG_PATH;
import static io.harness.common.CIExecutionConstants.TMP_PATH;

import com.google.inject.Inject;

import io.harness.beans.seriazlier.ProtobufSerializer;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.state.StepType;

import java.util.ArrayList;
import java.util.List;

public class SaveCacheStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SaveCacheStepInfo.typeInfo.getStepType();
  @Inject private ProtobufSerializer<SaveCacheStepInfo> runStepInfoProtobufSerializer;

  @Override
  protected List<String> getExecCommand(CIStepInfo ciStepInfo) {
    List<String> commands = new ArrayList<>();
    String command = String.format(LITE_ENGINE_STEP_COMMAND_FORMAT,
        runStepInfoProtobufSerializer.serialize((SaveCacheStepInfo) ciStepInfo), LOG_PATH, TMP_PATH);
    commands.add(command);
    return commands;
  }
}
