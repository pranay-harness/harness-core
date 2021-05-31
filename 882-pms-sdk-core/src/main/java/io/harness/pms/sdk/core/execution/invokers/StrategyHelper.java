package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.GeneralException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.EngineExceptionUtils;
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
        if (((ErrorResponseData) data).getException() == null) {
          throw new GeneralException(((ErrorResponseData) data).getErrorMessage());
        }
        throw((ErrorResponseData) data).getException();
      }
      return data;
    };
  }

  public StepResponse handleException(Exception ex) {
    List<ResponseMessage> responseMessages = exceptionManager.buildResponseFromException(ex);
    StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(Status.FAILED);
    FailureInfo failureInfo = EngineExceptionUtils.transformResponseMessagesToFailureInfo(responseMessages);
    return stepResponseBuilder.failureInfo(failureInfo).build();
  }
}
