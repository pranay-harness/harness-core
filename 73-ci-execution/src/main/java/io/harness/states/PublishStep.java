package io.harness.states;

import static io.harness.common.CIExecutionConstants.LITE_ENGINE_STEP_COMMAND_FORMAT;
import static io.harness.common.CIExecutionConstants.LOG_PATH;
import static io.harness.common.CIExecutionConstants.TMP_PATH;

import com.google.inject.Inject;

import io.harness.beans.seriazlier.ProtobufSerializer;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.state.StepType;

import java.util.ArrayList;
import java.util.List;

public class PublishStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = PublishStepInfo.typeInfo.getStepType();
  @Inject private ProtobufSerializer<PublishStepInfo> protobufSerializer;

  @Override
  protected List<String> getExecCommand(CIStepInfo ciStepInfo) {
    List<String> commands = new ArrayList<>();
    String command = String.format(LITE_ENGINE_STEP_COMMAND_FORMAT,
        protobufSerializer.serialize((PublishStepInfo) ciStepInfo), LOG_PATH, TMP_PATH);
    commands.add(command);
    return commands;
  }
}
