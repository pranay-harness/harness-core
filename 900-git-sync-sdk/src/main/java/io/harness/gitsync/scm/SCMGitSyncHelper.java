package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.GitSyncSdkModule.SCM_ON_DELEGATE;
import static io.harness.gitsync.GitSyncSdkModule.SCM_ON_MANAGER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.UserPrincipal;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.scm.beans.SCMNoOpResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class SCMGitSyncHelper {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  @Inject private EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  @Inject @Named(SCM_ON_MANAGER) private ScmGitHelper scmManagerGitHelper;
  @Inject @Named(SCM_ON_DELEGATE) private ScmGitHelper scmDelegateGitHelper;
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
          .build();
    }
    final InfoForGitPush infoForPush = getInfoForPush(gitBranchInfo, entityDetail);
    if (infoForPush.isExecuteOnDelegate()) {
      return scmDelegateGitHelper.pushToGitBasedOnChangeType(yaml, changeType, gitBranchInfo, infoForPush);
    } else {
      return scmManagerGitHelper.pushToGitBasedOnChangeType(yaml, changeType, gitBranchInfo, infoForPush);
    }
  }
  private InfoForGitPush getInfoForPush(GitEntityInfo gitBranchInfo, EntityDetail entityDetail) {
    final InfoForPush pushInfo = harnessToGitPushInfoServiceBlockingStub.getConnectorInfo(
        FileInfo.newBuilder()
            .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
            .setBranch(gitBranchInfo.getBranch())
            .setFolderPath(gitBranchInfo.getFolderPath())
            .setFilePath(gitBranchInfo.getFilePath())
            .setYamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
            .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
            .setUserPrincipal(getUserPrincipal())
            .build());
    if (!pushInfo.getStatus()) {
      throw new InvalidRequestException(pushInfo.getException().getValue());
    }
    final ScmConnector scmConnector =
        (ScmConnector) kryoSerializer.asObject(pushInfo.getConnector().getValue().toByteArray());
    List<EncryptedDataDetail> encryptedDataDetailList = null;
    if (pushInfo.getExecuteOnDelegate()) {
      encryptedDataDetailList = (List<EncryptedDataDetail>) kryoSerializer.asObject(
          pushInfo.getEncryptedDataDetails().getValue().toByteArray());
    }
    return InfoForGitPush.builder()
        .filePath(pushInfo.getFilePath().getValue())
        .folderPath(pushInfo.getFolderPath().getValue())
        .scmConnector(scmConnector)
        .projectIdentifier(pushInfo.getProjectIdentifier().getValue())
        .orgIdentifier(pushInfo.getOrgIdentifier().getValue())
        .accountId(pushInfo.getAccountId())
        .branch(gitBranchInfo.getBranch())
        .isDefault(pushInfo.getIsDefault())
        .yamlGitConfigId(pushInfo.getYamlGitConfigId())
        .isNewBranch(gitBranchInfo.isNewBranch())
        .defaultBranchName(pushInfo.getDefaultBranchName())
        .executeOnDelegate(pushInfo.getExecuteOnDelegate())
        .encryptedDataDetailList(encryptedDataDetailList)
        .build();
  }

  public UserPrincipal getUserPrincipal() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      io.harness.security.dto.UserPrincipal userPrincipal =
          (io.harness.security.dto.UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      return UserPrincipal.newBuilder()
          .setEmail(StringValue.of(userPrincipal.getEmail()))
          .setUserId(StringValue.of(userPrincipal.getName()))
          .build();
    }
    throw new InvalidRequestException("User not set for push event.");
  }
}
