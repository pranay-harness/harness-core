package io.harness.pms.sdk.core.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceImplBase;

import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;

@OwnedBy(PIPELINE)
@Singleton
public class JsonExpansionService extends JsonExpansionServiceImplBase {
  @Override
  public void expand(ExpansionRequestBatch request, StreamObserver<ExpansionResponseBatch> responseObserver) {
    // todo(@NamanVerma): add a proper implementation
    responseObserver.onNext(ExpansionResponseBatch.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
