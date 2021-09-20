package io.harness.pms.expressions.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.expression.LateBindingMap;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.expression.ExpressionRequest;
import io.harness.pms.contracts.expression.ExpressionResponse;
import io.harness.pms.contracts.expression.RemoteFunctorServiceGrpc.RemoteFunctorServiceBlockingStub;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.utils.PmsGrpcClientUtils;

import java.util.Collections;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class RemoteExpressionFunctor extends LateBindingMap implements ExpressionFunctor {
  private RemoteFunctorServiceBlockingStub remoteFunctorServiceBlockingStub;
  private String functorKey;
  public String value;
  Ambiance ambiance;

  @Override
  public Object get(Object args) {
    try {
      ExpressionResponse expressionResponse =
          PmsGrpcClientUtils.retryAndProcessException(remoteFunctorServiceBlockingStub::evaluate,
              ExpressionRequest.newBuilder()
                  .setAmbiance(ambiance)
                  .setFunctorKey(functorKey)
                  .addAllArgs(Collections.singletonList((String) args))
                  .build());
      if (expressionResponse.getIsPrimitive()) {
        return expressionResponse.getValue();
      }
      return RecastOrchestrationUtils.fromJson(expressionResponse.getValue());
    } catch (Exception ex) {
      log.error("Could not get object from remote functor for key: " + functorKey);
      throw ex;
    }
  }

  public Object getValue(String... args) {
    return get(args);
  }
}
