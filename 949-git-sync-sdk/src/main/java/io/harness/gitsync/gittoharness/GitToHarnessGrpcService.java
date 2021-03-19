package io.harness.gitsync.gittoharness;

import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceImplBase;
import io.harness.gitsync.ProcessingResponse;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitToHarnessGrpcService extends GitToHarnessServiceImplBase {
  @Override
  public void syncRequestFromGit(ChangeSet request, StreamObserver<ProcessingResponse> responseObserver) {
    log.info("grpc request done");
    responseObserver.onNext(ProcessingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
