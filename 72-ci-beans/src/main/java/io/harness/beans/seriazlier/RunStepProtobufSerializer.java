package io.harness.beans.seriazlier;

import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.StepContext;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class RunStepProtobufSerializer implements ProtobufSerializer<RunStepInfo> {
  @Override
  public String serialize(RunStepInfo object) {
    return Base64.encodeBase64String(convertRunStepInfo(object).toByteArray());
  }

  private Step convertRunStepInfo(RunStepInfo stepInfo) {
    RunStepInfo.Run run = stepInfo.getRun();
    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.addAllCommands(run.getCommand());
    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(stepInfo.getRetry())
                                  .setExecutionTimeoutSecs(stepInfo.getTimeout())
                                  .build());

    return Step.newBuilder()
        .setId(stepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(stepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .build();
  }
}
