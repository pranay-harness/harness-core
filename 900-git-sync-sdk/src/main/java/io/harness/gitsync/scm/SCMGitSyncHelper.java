package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.UserPrincipal;
import io.harness.gitsync.common.helper.ChangeTypeMapper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.scm.beans.SCMNoOpResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;

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
      final boolean defaultBranch =
          gitSyncSdkService.isDefaultBranch(entityDetail.getEntityRef().getAccountIdentifier(),
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

    final FileInfo fileInfo = FileInfo.newBuilder()
                                  .setUserPrincipal(getUserPrincipal())
                                  .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
                                  .setBranch(gitBranchInfo.getBranch())
                                  .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
                                  .setChangeType(ChangeTypeMapper.toProto(changeType))
                                  .setFilePath(gitBranchInfo.getFilePath())
                                  .setFolderPath(gitBranchInfo.getFolderPath())
                                  .setBaseBranch(StringValue.of(gitBranchInfo.getBaseBranch()))
                                  .setOldFileSha(StringValue.of(gitBranchInfo.getLastObjectId()))
                                  .setIsNewBranch(gitBranchInfo.isNewBranch())
                                  .setCommitMsg(StringValue.of(gitBranchInfo.getCommitMsg()))
                                  .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
                                  .putAllContextMap(MDC.getCopyOfContextMap())
                                  .setYaml(yaml)
                                  .build();
    final PushFileResponse pushFileResponse = harnessToGitPushInfoServiceBlockingStub.pushFile(fileInfo);
    // todo(abhinav): error handling

    return ScmGitUtils.createScmPushResponse(yaml, gitBranchInfo, pushFileResponse, entityDetail, changeType);
  }

  public UserPrincipal getUserPrincipal() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      io.harness.security.dto.UserPrincipal userPrincipal =
          (io.harness.security.dto.UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      return UserPrincipal.newBuilder()
          .setEmail(StringValue.of(userPrincipal.getEmail()))
          .setUserId(StringValue.of(userPrincipal.getName()))
          .setUserName(StringValue.of(userPrincipal.getUsername()))
          .build();
    }
    throw new InvalidRequestException("User not set for push event.");
  }
}
