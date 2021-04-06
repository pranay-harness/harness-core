package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class StrategyHelper {
  @Inject private ExceptionManager exceptionManager;

  public static ThrowingSupplier buildResponseDataSupplier(Map<String, ResponseData> responseDataMap) {
    return () -> {
      if (isEmpty(responseDataMap)) {
        return null;
      }
      ResponseData data = responseDataMap.values().iterator().next();
      if (data instanceof ErrorResponseData) {
        throw new ErrorDataException((ErrorResponseData) data);
      }
      return data;
    };
  }

  public StepResponse handleException(Exception ex) {
    // For Backward Compatibility extracting the first message and setting this
    // TODO (prashant) : Modify the failure info structure and adopt for arrays maintaining backward compatibility
    List<ResponseMessage> responseMessages = exceptionManager.buildResponseFromException(ex);
    StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED);
    if (!EmptyPredicate.isEmpty(responseMessages)) {
      ResponseMessage targetMessage = responseMessages.get(0);
      stepResponseBuilder.failureInfo(FailureInfo.newBuilder()
                                          .setErrorMessage(targetMessage.getMessage())
                                          .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                                              targetMessage.getFailureTypes()))
                                          .build());
    }
    return stepResponseBuilder.build();
  }
}
