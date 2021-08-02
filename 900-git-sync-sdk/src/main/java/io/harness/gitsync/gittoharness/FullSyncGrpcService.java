package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncServiceGrpc.FullSyncServiceImplBase;
import io.harness.gitsync.ScopeDetails;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class FullSyncGrpcService extends FullSyncServiceImplBase {
  @Override
  public void getEntitiesForFullSync(ScopeDetails request, StreamObserver<FileChanges> responseObserver) {
    // todo(abhinav): implement it.
  }
}
