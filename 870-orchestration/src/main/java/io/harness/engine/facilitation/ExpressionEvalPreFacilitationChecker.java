package io.harness.engine.facilitation;

import static io.harness.pms.contracts.execution.Status.FAILED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.OrchestrationEngine;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.ExceptionUtils;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.io.StepResponseProto;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ExpressionEvalPreFacilitationChecker extends AbstractPreFacilitationChecker {
  @Inject private OrchestrationEngine orchestrationEngine;

  ExecutionCheck handleExpressionEvaluationError(String nodeExecutionID, Exception ex) {
    String message = ExceptionUtils.getMessage(ex);
    StepResponseProto stepResponseProto =
        StepResponseProto.newBuilder()
            .setStatus(FAILED)
            .setFailureInfo(
                FailureInfo.newBuilder()
                    .setErrorMessage(String.format("Skip Condition Evaluation failed : %s", message))
                    .addFailureTypes(FailureType.APPLICATION_FAILURE)
                    .addFailureData(FailureData.newBuilder()
                                        .setMessage(String.format("Skip Condition Evaluation failed : %s", message))
                                        .setLevel(Level.ERROR.name())
                                        .setCode(ErrorCode.DEFAULT_ERROR_CODE.name())
                                        .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                        .build())
                    .build())
            .build();
    orchestrationEngine.handleStepResponse(nodeExecutionID, stepResponseProto);
    return ExecutionCheck.builder()
        .proceed(false)
        .reason("Error in evaluating configured when condition on step")
        .build();
  }
}
