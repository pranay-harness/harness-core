package io.harness.gitsync.server;

import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceImplBase;
import io.harness.gitsync.Test;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitToHarnessGrpcService extends GitToHarnessServiceImplBase {
  @Override
  public void syncRequestFromGit(Test request, StreamObserver<Test> responseObserver) {
    log.info("grpc request done");
    responseObserver.onNext(Test.newBuilder().build());
    responseObserver.onCompleted();
  }
}
