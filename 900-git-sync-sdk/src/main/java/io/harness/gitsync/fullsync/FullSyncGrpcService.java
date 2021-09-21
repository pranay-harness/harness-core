package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.AuthorizationServiceHeader;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc.FullSyncServiceImplBase;
import io.harness.gitsync.ScopeDetails;
import io.harness.logging.MdcContextSetter;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

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
      SecurityContextBuilder.setContext(
          new ServicePrincipal(AuthorizationServiceHeader.GIT_SYNC_SERVICE.getServiceId()));
      final FileChanges fileChanges = fullSyncSdkService.getFileChanges(request);
      responseObserver.onNext(fileChanges);
      responseObserver.onCompleted();
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
    }
  }

  @Override
  public void performEntitySync(FullSyncChangeSet request, StreamObserver<FullSyncResponse> responseObserver) {
    try (MdcContextSetter ignore1 = new MdcContextSetter(request.getLogContextMap())) {
      SecurityContextBuilder.setContext(
          new ServicePrincipal(AuthorizationServiceHeader.GIT_SYNC_SERVICE.getServiceId()));
      fullSyncSdkService.doFullSyncForFile(request);
      responseObserver.onNext(FullSyncResponse.newBuilder().setSuccess(true).build());
    } catch (Exception e) {
      log.error("Error while doing full sync", e);
      responseObserver.onNext(
          FullSyncResponse.newBuilder().setSuccess(false).setErrorMsg(ExceptionUtils.getMessage(e)).build());
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
    }
    responseObserver.onCompleted();
  }
}
