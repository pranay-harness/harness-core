package io.harness.gitsync.server;

import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceImplBase;
import io.harness.gitsync.ProcessingResponse;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitToHarnessGrpcService extends GitToHarnessServiceImplBase {
  // todo(abhinav): remove and add to other place.
  @Override
  public void syncRequestFromGit(ChangeSet request, StreamObserver<ProcessingResponse> responseObserver) {
    log.info("grpc request done");
    responseObserver.onNext(ProcessingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
