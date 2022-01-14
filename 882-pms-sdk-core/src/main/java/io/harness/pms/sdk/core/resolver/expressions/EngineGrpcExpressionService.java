package io.harness.pms.sdk.core.resolver.expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobRequest;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobResponse;
import io.harness.pms.contracts.service.ExpressionRenderBlobRequest;
import io.harness.pms.contracts.service.ExpressionRenderBlobResponse;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
    ExpressionRenderBlobResponse expressionRenderBlobResponse =
        PmsGrpcClientUtils.retryAndProcessException(engineExpressionProtoServiceBlockingStub::renderExpression,
            ExpressionRenderBlobRequest.newBuilder()
                .setAmbiance(ambiance)
                .setExpression(expression)
                .setSkipUnresolvedExpressionsCheck(skipUnresolvedExpressionsCheck)
                .build());
    return expressionRenderBlobResponse.getValue();
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    ExpressionEvaluateBlobResponse expressionEvaluateBlobResponse =
        PmsGrpcClientUtils.retryAndProcessException(engineExpressionProtoServiceBlockingStub::evaluateExpression,
            ExpressionEvaluateBlobRequest.newBuilder().setAmbiance(ambiance).setExpression(expression).build());
    return RecastOrchestrationUtils.fromJson(expressionEvaluateBlobResponse.getValue(), Object.class);
  }
}
