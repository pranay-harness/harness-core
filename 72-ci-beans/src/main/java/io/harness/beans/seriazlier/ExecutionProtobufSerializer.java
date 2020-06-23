package io.harness.beans.seriazlier;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ExecutionProtobufSerializer implements ProtobufSerializer<Execution> {
  @Override
  public String serialize(Execution object) {
    List<ExecutionSection> steps = object.getSteps();
    List<Step> protoSteps = new LinkedList<>();
    for (ExecutionSection step : steps) {
      if (step instanceof CIStepInfo) {
        CIStepInfo ciStepInfo = (CIStepInfo) step;

        Step.Builder protoStepBuilder = Step.newBuilder();

        // Protobuf setter do not allow setting null values so optional fields are only set when they have value
        Optional.ofNullable(ciStepInfo.getDisplayName()).ifPresent(protoStepBuilder::setDisplayName);

        protoStepBuilder.setId(ciStepInfo.getIdentifier());

        switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
          case RUN:

            protoSteps.add(protoStepBuilder.setRun(convertRunStepInfo((RunStepInfo) ciStepInfo)).build());
            break;
          case BUILD:
          case TEST:
          case SETUP_ENV:
          case CLEANUP:
          case PUBLISH:
          case GIT_CLONE:
          case LITE_ENGINE_TASK:
          default:
            logger.info("not implemented");
        }
      }
    }

    return Base64.encodeBase64String(
        io.harness.product.ci.engine.proto.Execution.newBuilder().addAllSteps(protoSteps).build().toByteArray());
  }

  private RunStep convertRunStepInfo(RunStepInfo stepInfo) {
    RunStepInfo.Run run = stepInfo.getRun();

    RunStep.Builder runStepBuilder = RunStep.newBuilder();

    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(stepInfo.getRetry())
                                  .setExecutionTimeoutSecs(stepInfo.getTimeout())
                                  .build());

    runStepBuilder.addAllCommands(run.getCommand());
    return runStepBuilder.build();
  }
}
