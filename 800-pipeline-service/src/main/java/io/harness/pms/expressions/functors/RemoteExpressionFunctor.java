package io.harness.pms.expressions.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.expression.ExpressionRequest;
import io.harness.pms.contracts.expression.RemoteFunctorServiceGrpc.RemoteFunctorServiceBlockingStub;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.utils.PmsGrpcClientUtils;

import java.util.Arrays;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class RemoteExpressionFunctor implements ExpressionFunctor {
  private RemoteFunctorServiceBlockingStub remoteFunctorServiceBlockingStub;
  private String functorKey;
  public String value;
  Ambiance ambiance;

  public Object get(String... args) {
    try {
      String response =
          PmsGrpcClientUtils
              .retryAndProcessException(remoteFunctorServiceBlockingStub::evaluate,
                  ExpressionRequest.newBuilder().setFunctorKey(functorKey).addAllArgs(Arrays.asList(args)).build())
              .getValue();
      return RecastOrchestrationUtils.fromJson(response);
    } catch (Exception ex) {
      log.error("Could not get object from remote functor for key: " + functorKey);
      throw ex;
    }
  }
}
