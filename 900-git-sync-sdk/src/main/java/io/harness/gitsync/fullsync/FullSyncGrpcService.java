/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper.mapEventToRestEntityType;

import static java.util.stream.Collectors.toCollection;

import io.harness.AuthorizationServiceHeader;
import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.FullSyncFileResponse;
import io.harness.gitsync.FullSyncRequest;
import io.harness.gitsync.FullSyncResponse;
import io.harness.gitsync.FullSyncServiceGrpc.FullSyncServiceImplBase;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.logging.MdcContextSetter;
import io.harness.manage.GlobalContextManager;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class FullSyncGrpcService extends FullSyncServiceImplBase {
  FullSyncSdkService fullSyncSdkService;
  Supplier<List<EntityType>> sortOrder;

  @Inject
  public FullSyncGrpcService(
      FullSyncSdkService fullSyncSdkService, @Named("GitSyncSortOrder") Supplier<List<EntityType>> sortOrder) {
    this.fullSyncSdkService = fullSyncSdkService;
    this.sortOrder = sortOrder;
  }

  @Override
  public void getEntitiesForFullSync(ScopeDetails request, StreamObserver<FileChanges> responseObserver) {
    log.info("Got the Grpc Request for Full Sync");
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
  public void performEntitySync(FullSyncRequest request, StreamObserver<FullSyncResponse> responseObserver) {
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard();
         MdcContextSetter ignore1 = new MdcContextSetter(request.getLogContextMap())) {
      List<FullSyncChangeSet> fileChangesList =
          request.getFileChangesList().stream().collect(toCollection(ArrayList::new));
      List<EntityType> entityTypesOrder = emptyIfNull(sortOrder.get());
      emptyIfNull(fileChangesList)
          .sort(Comparator.comparingInt(
              f -> entityTypesOrder.indexOf(mapEventToRestEntityType(f.getEntityDetail().getType()))));
      SourcePrincipalContextBuilder.setSourcePrincipal(
          new ServicePrincipal(AuthorizationServiceHeader.GIT_SYNC_SERVICE.getServiceId()));
      List<FullSyncFileResponse> fullSyncFileResponses = new ArrayList<>();
      for (FullSyncChangeSet fileChangeSet : fileChangesList) {
        try {
          fullSyncSdkService.doFullSyncForFile(fileChangeSet);
          fullSyncFileResponses.add(
              FullSyncFileResponse.newBuilder().setSuccess(true).setFilePath(fileChangeSet.getFilePath()).build());
        } catch (Exception e) {
          log.error("Error while doing full sync", e);
          fullSyncFileResponses.add(
              FullSyncFileResponse.newBuilder().setSuccess(false).setErrorMsg(ExceptionUtils.getMessage(e)).build());
        }
      }
      responseObserver.onNext(FullSyncResponse.newBuilder().addAllFileResponse(fullSyncFileResponses).build());
    } finally {
      GlobalContextManager.unset();
    }
    responseObserver.onCompleted();
  }

  private GitSyncBranchContext createGitEntityInfo(FullSyncChangeSet request) {
    GitEntityInfo gitEntityInfo = GitEntityInfo.builder()
                                      .filePath(request.getFilePath())
                                      .folderPath(request.getFolderPath())
                                      .yamlGitConfigId(request.getYamlGitConfigIdentifier())
                                      .branch(request.getBranchName())
                                      .isFullSyncFlow(true)
                                      .build();
    return GitSyncBranchContext.builder().gitBranchInfo(gitEntityInfo).build();
  }
}
