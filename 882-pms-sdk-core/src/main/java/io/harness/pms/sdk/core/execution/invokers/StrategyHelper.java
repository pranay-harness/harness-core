package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
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
  @Inject private ResponseDataMapper responseDataMapper;

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
      FailureData failureData = failureDataList.get(0);
      failureInfoBuilder.setErrorMessage(emptyIfNull(failureData.getMessage()))
          .addAllFailureTypes(failureData.getFailureTypesList());
    }
    return stepResponseBuilder.failureInfo(failureInfoBuilder.build()).build();
  }

  public QueueNodeExecutionRequest getQueueNodeExecutionRequest(NodeExecutionProto nodeExecution) {
    return QueueNodeExecutionRequest.newBuilder().setNodeExecution(nodeExecution).build();
  }

  public AddExecutableResponseRequest getAddExecutableResponseRequest(
      String nodeExecutionId, Status status, ExecutableResponse executableResponse, List<String> callbackIds) {
    return AddExecutableResponseRequest.newBuilder()
        .setNodeExecutionId(nodeExecutionId)
        .setStatus(status)
        .setExecutableResponse(executableResponse)
        .addAllCallbackIds(callbackIds)
        .build();
  }
}
