package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc.FullSyncServiceImplBase;
import io.harness.gitsync.ScopeDetails;
import io.harness.logging.MdcContextSetter;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class FullSyncGrpcService extends FullSyncServiceImplBase {
  FullSyncSdkService fullSyncSdkService;

  @Override
  public void getEntitiesForFullSync(ScopeDetails request, StreamObserver<FileChanges> responseObserver) {
    try (MdcContextSetter ignore1 = new MdcContextSetter(request.getLogContextMap())) {
      final FileChanges fileChanges = fullSyncSdkService.getFileChanges(request);
      responseObserver.onNext(fileChanges);
      responseObserver.onCompleted();
    }
  }

  public void performEntitySync(FullSyncChangeSet request, StreamObserver<FullSyncResponse> responseObserver) {}
}
