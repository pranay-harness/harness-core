package io.harness.gitsync.sdk;

import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceImplBase;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.PushResponse;

import io.grpc.stub.StreamObserver;

public class HarnessToGitPushInfoGrpcService extends HarnessToGitPushInfoServiceImplBase {
  @Override
  public void pushFromHarness(PushInfo request, StreamObserver<PushResponse> responseObserver) {
    responseObserver.onNext(PushResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void getConnectorInfo(FileInfo request, StreamObserver<InfoForPush> responseObserver) {
    responseObserver.onNext(InfoForPush.newBuilder().build());
    responseObserver.onCompleted();
  }
}
