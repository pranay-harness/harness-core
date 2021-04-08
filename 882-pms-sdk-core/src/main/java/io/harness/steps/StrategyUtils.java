package io.harness.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.ResumeNodeExecutionRequest;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class StrategyUtils {
  @Inject private ResponseDataMapper responseDataMapper;

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

  public ResumeNodeExecutionRequest getResumeNodeExecutionRequest(
      String nodeExecutionId, Map<String, ResponseData> response, boolean asyncError) {
    Map<String, ByteString> responseBytes = responseDataMapper.toResponseDataProto(response);

    return ResumeNodeExecutionRequest.newBuilder()
        .setNodeExecutionId(nodeExecutionId)
        .putAllResponse(responseBytes)
        .setAsyncError(asyncError)
        .build();
  }
}
