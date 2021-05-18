package io.harness.pms.sdk.core.resolver.expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobRequest;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobResponse;
import io.harness.pms.contracts.service.ExpressionRenderBlobRequest;
import io.harness.pms.contracts.service.ExpressionRenderBlobResponse;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.StatusRuntimeException;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class EngineGrpcExpressionService implements EngineExpressionService {
  private final EngineExpressionProtoServiceBlockingStub engineExpressionProtoServiceBlockingStub;

  @Inject
  public EngineGrpcExpressionService(
      EngineExpressionProtoServiceBlockingStub engineExpressionProtoServiceBlockingStub) {
    this.engineExpressionProtoServiceBlockingStub = engineExpressionProtoServiceBlockingStub;
  }

  @Override
  public String renderExpression(Ambiance ambiance, String expression, boolean skipUnresolvedExpressionsCheck) {
    try {
      ExpressionRenderBlobResponse expressionRenderBlobResponse =
          engineExpressionProtoServiceBlockingStub.renderExpression(
              ExpressionRenderBlobRequest.newBuilder()
                  .setAmbiance(ambiance)
                  .setExpression(expression)
                  .setSkipUnresolvedExpressionsCheck(skipUnresolvedExpressionsCheck)
                  .build());
      return expressionRenderBlobResponse.getValue();
    } catch (StatusRuntimeException ex) {
      throw new GeneralException(ex.getStatus().getDescription());
    }
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    try {
      ExpressionEvaluateBlobResponse expressionEvaluateBlobResponse =
          engineExpressionProtoServiceBlockingStub.evaluateExpression(
              ExpressionEvaluateBlobRequest.newBuilder().setAmbiance(ambiance).setExpression(expression).build());
      return RecastOrchestrationUtils.fromDocumentJson(expressionEvaluateBlobResponse.getValue(), Object.class);
    } catch (StatusRuntimeException ex) {
      throw new GeneralException(ex.getStatus().getDescription());
    }
  }
}
