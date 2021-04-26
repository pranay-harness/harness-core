package io.harness.engine.facilitation;

import static io.harness.pms.contracts.execution.Status.FAILED;

import io.harness.engine.OrchestrationEngine;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.io.StepResponseProto;

import com.google.inject.Inject;

public abstract class ExpressionEvalPreFacilitationChecker extends AbstractPreFacilitationChecker {
  @Inject private OrchestrationEngine orchestrationEngine;

  PreFacilitationCheck handleExpressionEvaluationError(String nodeExecutionID, Exception ex) {
    StepResponseProto stepResponseProto =
        StepResponseProto.newBuilder()
            .setStatus(FAILED)
            .setFailureInfo(
                FailureInfo.newBuilder()
                    .setErrorMessage(String.format("Skip Condition Evaluation failed : %s", ex.getMessage()))
                    .addFailureTypes(FailureType.APPLICATION_FAILURE)
                    .addFailureData(
                        FailureData.newBuilder()
                            .setMessage(String.format("Skip Condition Evaluation failed : %s", ex.getMessage()))
                            .setLevel(Level.ERROR.name())
                            .setCode(ErrorCode.DEFAULT_ERROR_CODE.name())
                            .addFailureTypes(FailureType.APPLICATION_FAILURE)
                            .build())
                    .build())
            .build();
    orchestrationEngine.handleStepResponse(nodeExecutionID, stepResponseProto);
    return PreFacilitationCheck.builder()
        .proceed(false)
        .reason("Error in evaluating configured when condition on step")
        .build();
  }
}
