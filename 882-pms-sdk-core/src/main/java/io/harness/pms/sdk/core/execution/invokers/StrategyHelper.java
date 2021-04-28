package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
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
import java.util.stream.Collectors;

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
    List<FailureData> failureDataList =
        responseMessages.stream()
            .map(rm
                -> FailureData.newBuilder()
                       .setCode(rm.getCode().name())
                       .setLevel(rm.getLevel().name())
                       .setMessage(emptyIfNull(rm.getMessage()))
                       .addAllFailureTypes(
                           EngineExceptionUtils.transformToOrchestrationFailureTypes(rm.getFailureTypes()))
                       .build())
            .collect(Collectors.toList());

    FailureInfo.Builder failureInfoBuilder = FailureInfo.newBuilder().addAllFailureData(failureDataList);
    if (!EmptyPredicate.isEmpty(failureDataList)) {
      FailureData failureData = failureDataList.get(failureDataList.size() - 1);
      failureInfoBuilder.setErrorMessage(emptyIfNull(failureData.getMessage()))
          .addAllFailureTypes(failureData.getFailureTypesList());
    }

    TaskNGDataException taskFailureData = ExceptionUtils.cause(TaskNGDataException.class, ex);
    if (taskFailureData != null) {
      stepResponseBuilder.unitProgressList(taskFailureData.getCommandUnitsProgress().getUnitProgresses());
    }

    return stepResponseBuilder.failureInfo(failureInfoBuilder.build()).build();
  }
}
