package io.harness.pms.sdk.core.execution.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.expression.ErrorResponse;
import io.harness.pms.contracts.expression.ExpressionRequest;
import io.harness.pms.contracts.expression.ExpressionResponse;
import io.harness.pms.contracts.expression.RemoteFunctorServiceGrpc.RemoteFunctorServiceImplBase;
import io.harness.pms.sdk.core.registries.FunctorRegistry;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ProtocolStringList;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class RemoteFunctorService extends RemoteFunctorServiceImplBase {
  @Inject FunctorRegistry functorRegistry;
  @Inject ExceptionManager exceptionManager;

  @Override
  public void evaluate(ExpressionRequest request, StreamObserver<ExpressionResponse> responseObserver) {
    ExpressionResponse expressionResponse;
    try {
      SdkFunctor functor = functorRegistry.obtain(request.getFunctorKey());
      ProtocolStringList argsList = request.getArgsList();
      ExpressionResult expressionResult = functor.get(request.getAmbiance(), argsList.toArray(new String[0]));
      expressionResponse =
          ExpressionResponse.newBuilder().setValue(RecastOrchestrationUtils.toJson(expressionResult)).build();
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex), ex);
      WingsException processedException = exceptionManager.processException(ex);
      expressionResponse =
          ExpressionResponse.newBuilder()
              .setErrorResponse(
                  ErrorResponse.newBuilder().addMessages(ExceptionUtils.getMessage(processedException)).build())
              .build();
    }

    responseObserver.onNext(expressionResponse);
    responseObserver.onCompleted();
  }
}
