package io.harness.grpc.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dms.CrudEventRequest;
import io.harness.dms.CrudEventResponse;
import io.harness.dms.DmsServiceGrpc;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DEL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class DmsServiceGrpcImpl extends DmsServiceGrpc.DmsServiceImplBase {
  Set<DmsRequestHandler> dmsRequestHandlers;

  @Override
  public void crudEvent(CrudEventRequest request, StreamObserver<CrudEventResponse> responseObserver) {
    try {
      final CrudEventResponse crudEventResponse = handDmsRequest(request);
      responseObserver.onNext(crudEventResponse);
      responseObserver.onCompleted();

    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing crud request [{}].", request, ex);
      responseObserver.onError(
          io.grpc.Status.INTERNAL.withDescription(ExceptionUtils.getMessage(ex)).asRuntimeException());
    }
  }

  public CrudEventResponse handDmsRequest(CrudEventRequest request) {
    final Optional<DmsRequestHandler> dmsRequestHandler =
        dmsRequestHandlers.stream().filter(h -> h.canHandle(request)).findFirst();
    return dmsRequestHandler.map(handler -> handler.handle(request))
        .orElseThrow(
            () -> new InvalidRequestException(String.format("No Handler found for given request [%s]", request)));
  }
}
