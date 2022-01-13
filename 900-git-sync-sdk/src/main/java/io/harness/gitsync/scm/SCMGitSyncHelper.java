/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ScmException;
import io.harness.exception.WingsException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.Principal;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.UserPrincipal;
import io.harness.gitsync.common.helper.ChangeTypeMapper;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.exceptions.GitSyncException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.scm.beans.SCMNoOpResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Singleton
@Slf4j
@OwnedBy(DX)
public class SCMGitSyncHelper {
  @Inject private HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  @Inject private EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  @Inject GitSyncSdkService gitSyncSdkService;

  public ScmPushResponse pushToGit(
      GitEntityInfo gitBranchInfo, String yaml, ChangeType changeType, EntityDetail entityDetail) {
    if (gitBranchInfo.isSyncFromGit()) {
      return getResponseInG2H(gitBranchInfo, entityDetail);
    }

    final FileInfo fileInfo = getFileInfo(gitBranchInfo, yaml, changeType, entityDetail);

    final PushFileResponse pushFileResponse =
        GitSyncGrpcClientUtils.retryAndProcessException(harnessToGitPushInfoServiceBlockingStub::pushFile, fileInfo);
    try {
      checkForError(pushFileResponse);
    } catch (ScmException e) {
      throwDifferentExceptionInCaseOfChangeTypeAdd(gitBranchInfo, changeType, e);
    }
    return ScmGitUtils.createScmPushResponse(yaml, gitBranchInfo, pushFileResponse, entityDetail, changeType);
  }

  private void throwDifferentExceptionInCaseOfChangeTypeAdd(
      GitEntityInfo gitBranchInfo, ChangeType changeType, ScmException e) {
    if (changeType.equals(ChangeType.ADD)) {
      final WingsException cause = ExceptionUtils.cause(ErrorCode.SCM_CONFLICT_ERROR, e);
      if (cause != null) {
        throw new InvalidRequestException(String.format(
            "A file with name %s already exists in the remote Git repository", gitBranchInfo.getFilePath()));
      }
      throw e;
    }
  }

  private FileInfo getFileInfo(
      GitEntityInfo gitBranchInfo, String yaml, ChangeType changeType, EntityDetail entityDetail) {
    FileInfo.Builder builder = FileInfo.newBuilder()
                                   .setPrincipal(getPrincipal())
                                   .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
                                   .setBranch(gitBranchInfo.getBranch())
                                   .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
                                   .setChangeType(ChangeTypeMapper.toProto(changeType))
                                   .setFilePath(gitBranchInfo.getFilePath())
                                   .setFolderPath(gitBranchInfo.getFolderPath())
                                   .setIsNewBranch(gitBranchInfo.isNewBranch())
                                   .setCommitMsg(StringValue.of(gitBranchInfo.getCommitMsg()))
                                   .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
                                   .putAllContextMap(MDC.getCopyOfContextMap())
                                   .setYaml(emptyIfNull(yaml));
    if (gitBranchInfo.getBaseBranch() != null) {
      builder.setBaseBranch(StringValue.of(gitBranchInfo.getBaseBranch()));
    }

    if (gitBranchInfo.getLastObjectId() != null) {
      builder.setOldFileSha(StringValue.of(gitBranchInfo.getLastObjectId()));
    }
    return builder.build();
  }

  private SCMNoOpResponse getResponseInG2H(GitEntityInfo gitBranchInfo, EntityDetail entityDetail) {
    final boolean defaultBranch = gitSyncSdkService.isDefaultBranch(entityDetail.getEntityRef().getAccountIdentifier(),
        entityDetail.getEntityRef().getOrgIdentifier(), entityDetail.getEntityRef().getProjectIdentifier());
    return SCMNoOpResponse.builder()
        .filePath(gitBranchInfo.getFilePath())
        .pushToDefaultBranch(defaultBranch)
        .objectId(gitBranchInfo.getLastObjectId())
        .yamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
        .branch(gitBranchInfo.getBranch())
        .folderPath(gitBranchInfo.getFolderPath())
        .commitId(gitBranchInfo.getCommitId())
        .build();
  }

  private void checkForError(PushFileResponse pushFileResponse) {
    if (pushFileResponse.getStatus() != 1) {
      final String errorMessage =
          isNotEmpty(pushFileResponse.getError()) ? pushFileResponse.getError() : "Error in doing git push";
      throw new GitSyncException(errorMessage);
    }
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
        pushFileResponse.getScmResponseCode(), pushFileResponse.getError());
  }

  private Principal getPrincipal() {
    final io.harness.security.dto.Principal sourcePrincipal = SourcePrincipalContextBuilder.getSourcePrincipal();
    if (sourcePrincipal == null) {
      throw new InvalidRequestException("Principal cannot be null");
    }
    final Principal.Builder principalBuilder = Principal.newBuilder();
    switch (sourcePrincipal.getType()) {
      case USER:
        io.harness.security.dto.UserPrincipal userPrincipalFromContext =
            (io.harness.security.dto.UserPrincipal) sourcePrincipal;
        final UserPrincipal userPrincipal = UserPrincipal.newBuilder()
                                                .setEmail(StringValue.of(userPrincipalFromContext.getEmail()))
                                                .setUserId(StringValue.of(userPrincipalFromContext.getName()))
                                                .setUserName(StringValue.of(userPrincipalFromContext.getUsername()))
                                                .build();
        return principalBuilder.setUserPrincipal(userPrincipal).build();
      case SERVICE:
        final ServicePrincipal servicePrincipalFromContext = (ServicePrincipal) sourcePrincipal;
        final io.harness.gitsync.ServicePrincipal servicePrincipal =
            io.harness.gitsync.ServicePrincipal.newBuilder().setName(servicePrincipalFromContext.getName()).build();
        return principalBuilder.setServicePrincipal(servicePrincipal).build();
      default:
        throw new InvalidRequestException("Principal type not set.");
    }
  }
}
