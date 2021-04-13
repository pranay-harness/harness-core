package io.harness.engine.executions.node;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.InvocationHelper;
import io.harness.pms.contracts.plan.AccumulateResponsesRequest;
import io.harness.pms.contracts.plan.AccumulateResponsesResponse;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsNodeExecutionGrpcSevice extends NodeExecutionProtoServiceImplBase {
  @Inject private ResponseDataMapper responseDataMapper;
  @Inject private InvocationHelper invocationHelper;

  @Override
  public void accumulateResponses(
      AccumulateResponsesRequest request, StreamObserver<AccumulateResponsesResponse> responseObserver) {
    Map<String, ResponseData> responseDataMap =
        invocationHelper.accumulateResponses(request.getPlanExecutionId(), request.getNotifyId());
    Map<String, ByteString> response = responseDataMapper.toResponseDataProto(responseDataMap);
    responseObserver.onNext(AccumulateResponsesResponse.newBuilder().putAllResponse(response).build());
    responseObserver.onCompleted();
  }
}
