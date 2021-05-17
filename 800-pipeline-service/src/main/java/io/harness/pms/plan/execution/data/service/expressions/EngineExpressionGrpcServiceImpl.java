package io.harness.pms.plan.execution.data.service.expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceImplBase;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobRequest;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobResponse;
import io.harness.pms.contracts.service.ExpressionRenderBlobRequest;
import io.harness.pms.contracts.service.ExpressionRenderBlobResponse;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class EngineExpressionGrpcServiceImpl extends EngineExpressionProtoServiceImplBase {
  private final EngineExpressionService engineExpressionService;

  @Inject
  public EngineExpressionGrpcServiceImpl(EngineExpressionService engineExpressionService) {
    this.engineExpressionService = engineExpressionService;
  }

  @Override
  public void renderExpression(
      ExpressionRenderBlobRequest request, StreamObserver<ExpressionRenderBlobResponse> responseObserver) {
    String value = engineExpressionService.renderExpression(
        request.getAmbiance(), request.getExpression(), request.getSkipUnresolvedExpressionsCheck());
    responseObserver.onNext(ExpressionRenderBlobResponse.newBuilder().setValue(value).build());
    responseObserver.onCompleted();
  }

  @Override
  public void evaluateExpression(
      ExpressionEvaluateBlobRequest request, StreamObserver<ExpressionEvaluateBlobResponse> responseObserver) {
    Object value = engineExpressionService.evaluateExpression(request.getAmbiance(), request.getExpression());
    responseObserver.onNext(
        ExpressionEvaluateBlobResponse.newBuilder().setValue(RecastOrchestrationUtils.toDocumentJson(value)).build());
    responseObserver.onCompleted();
  }
}
