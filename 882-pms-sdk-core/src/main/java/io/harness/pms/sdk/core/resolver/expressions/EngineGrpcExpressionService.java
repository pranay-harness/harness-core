package io.harness.pms.sdk.core.resolver.expressions;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobRequest;
import io.harness.pms.contracts.service.ExpressionEvaluateBlobResponse;
import io.harness.pms.contracts.service.ExpressionRenderBlobRequest;
import io.harness.pms.contracts.service.ExpressionRenderBlobResponse;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EngineGrpcExpressionService implements EngineExpressionService {
  private final EngineExpressionProtoServiceBlockingStub engineExpressionProtoServiceBlockingStub;

  @Inject
  public EngineGrpcExpressionService(
      EngineExpressionProtoServiceBlockingStub engineExpressionProtoServiceBlockingStub) {
    this.engineExpressionProtoServiceBlockingStub = engineExpressionProtoServiceBlockingStub;
  }

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    ExpressionRenderBlobResponse expressionRenderBlobResponse =
        engineExpressionProtoServiceBlockingStub.renderExpression(
            ExpressionRenderBlobRequest.newBuilder().setAmbiance(ambiance).setExpression(expression).build());
    return expressionRenderBlobResponse.getValue();
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    ExpressionEvaluateBlobResponse expressionEvaluateBlobResponse =
        engineExpressionProtoServiceBlockingStub.evaluateExpression(
            ExpressionEvaluateBlobRequest.newBuilder().setAmbiance(ambiance).setExpression(expression).build());
    return DocumentOrchestrationUtils.convertFromDocumentJson(expressionEvaluateBlobResponse.getValue());
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o) {
    throw new InvalidRequestException("Resolve method in Grpc is not supported");
  }
}
