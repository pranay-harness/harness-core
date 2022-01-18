/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.sdk;

import static io.harness.AuthorizationServiceHeader.GIT_SYNC_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.gitsync.BranchDetails;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceImplBase;
import io.harness.gitsync.IsGitSyncEnabled;
import io.harness.gitsync.Principal;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.PushResponse;
import io.harness.gitsync.RepoDetails;
import io.harness.gitsync.ServicePrincipal;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.logging.MdcContextSetter;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.UserPrincipal;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class HarnessToGitPushInfoGrpcService extends HarnessToGitPushInfoServiceImplBase {
  @Inject HarnessToGitHelperService harnessToGitHelperService;
  @Inject KryoSerializer kryoSerializer;
  @Inject EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  @Inject ExceptionManager exceptionManager;

  @Override
  public void pushFromHarness(PushInfo request, StreamObserver<PushResponse> responseObserver) {
    try (MdcContextSetter ignore1 = new MdcContextSetter(request.getContextMapMap())) {
      log.debug("Grpc request received for pushFromHarness");
      harnessToGitHelperService.postPushOperation(request);
      responseObserver.onNext(PushResponse.newBuilder().build());
      responseObserver.onCompleted();
      log.debug("Grpc request completed for pushFromHarness");
    }
  }

  @Override
  public void pushFile(FileInfo request, StreamObserver<PushFileResponse> responseObserver) {
    PushFileResponse pushFileResponse;
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard();
         MdcContextSetter ignore1 = new MdcContextSetter(request.getContextMapMap())) {
      log.debug("Grpc request received for pushFile");
      setPrincipal(request);
      pushFileResponse = harnessToGitHelperService.pushFile(request);
      log.debug("Grpc request completed for pushFile");
    } catch (Exception e) {
      log.error("Push to git failed with exception", e);
      final String message = ExceptionUtils.getMessage(e);
      pushFileResponse = PushFileResponse.newBuilder()
                             .setDefaultBranchName("")
                             .setIsDefault(false)
                             .setScmResponseCode(-1)
                             .setCommitId("")
                             .setError(message)
                             .setStatus(0)
                             .setDefaultBranchName("")
                             .build();
    }
    responseObserver.onNext(pushFileResponse);
    responseObserver.onCompleted();
  }

  private void setPrincipal(FileInfo request) {
    final io.harness.security.dto.Principal principal = getPrincipal(request);
    GlobalContextManager.upsertGlobalContextRecord(SourcePrincipalContextData.builder().principal(principal).build());
  }

  private io.harness.security.dto.Principal getPrincipal(FileInfo request) {
    final Principal principalFromProto = request.getPrincipal();
    if (principalFromProto.hasUserPrincipal()) {
      final io.harness.gitsync.UserPrincipal userPrincipal = principalFromProto.getUserPrincipal();
      return new UserPrincipal(userPrincipal.getUserId().getValue(), userPrincipal.getEmail().getValue(),
          userPrincipal.getUserName().getValue(), request.getAccountId());
    } else if (principalFromProto.hasServicePrincipal()) {
      checkIfServicePrincipalIsValid(request.getPrincipal().getServicePrincipal());
      return new io.harness.security.dto.ServicePrincipal(request.getPrincipal().getServicePrincipal().getName());
    }
    throw new InvalidRequestException("Only service or user principal allowed");
  }

  private void checkIfServicePrincipalIsValid(ServicePrincipal servicePrincipal) {
    if (!GIT_SYNC_SERVICE.getServiceId().equals(servicePrincipal.getName())) {
      throw new InvalidRequestException("Only git sync service principal is allowed for push");
    }
  }

  @Override
  public void isGitSyncEnabledForScope(EntityScopeInfo request, StreamObserver<IsGitSyncEnabled> responseObserver) {
    log.debug("Grpc request received for isGitSyncEnabledForScope");
    final Boolean gitSyncEnabled = harnessToGitHelperService.isGitSyncEnabled(request);
    responseObserver.onNext(IsGitSyncEnabled.newBuilder().setEnabled(gitSyncEnabled).build());
    responseObserver.onCompleted();
    log.debug("Grpc request completed for isGitSyncEnabledForScope");
  }

  @Override
  public void getDefaultBranch(RepoDetails request, StreamObserver<BranchDetails> responseObserver) {
    try (MdcContextSetter ignore1 = new MdcContextSetter(request.getContextMapMap())) {
      log.debug("Grpc request received for getDefaultBranch");
      final BranchDetails branchDetails = harnessToGitHelperService.getBranchDetails(request);
      responseObserver.onNext(branchDetails);
      responseObserver.onCompleted();
      log.debug("Grpc request completed for getDefaultBranch");
    }
  }
}
