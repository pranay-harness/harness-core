package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.GitToHarnessServiceGrpc.GitToHarnessServiceImplBase;
import io.harness.gitsync.ProcessingResponse;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class GitToHarnessGrpcService extends GitToHarnessServiceImplBase {
  @Inject GitToHarnessProcessor gitToHarnessProcessor;

  @Override
  public void syncRequestFromGit(ChangeSet request, StreamObserver<ProcessingResponse> responseObserver) {
    log.info("grpc request done");
    responseObserver.onNext(ProcessingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void process(
      GitToHarnessProcessRequest gitToHarnessRequest, StreamObserver<ProcessingResponse> responseObserver) {
    // todo: add proper ids so that we can check the git flows
    log.info("Grpc request recieved");
    gitToHarnessProcessor.gitToHarnessProcessingRequest(gitToHarnessRequest);
    responseObserver.onNext(ProcessingResponse.newBuilder().build());
    responseObserver.onCompleted();
    log.info("Grpc request completed");
  }
}