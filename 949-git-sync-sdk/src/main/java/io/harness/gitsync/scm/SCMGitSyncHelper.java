package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFileDetails.GitFileDetailsBuilder;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.InfoForPush;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.helpers.ScmUserHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.scm.beans.SCMNoOpResponse;
import io.harness.gitsync.scm.beans.ScmCreateFileResponse;
import io.harness.gitsync.scm.beans.ScmDeleteFileResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.gitsync.scm.beans.ScmUpdateFileResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@Slf4j
@OwnedBy(DX)
public class SCMGitSyncHelper {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  @Inject private EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;
  @Inject private ScmClient scmClient;

  public ScmPushResponse pushToGit(
      GitEntityInfo gitBranchInfo, String yaml, ChangeType changeType, EntityDetail entityDetail) {
    final InfoForGitPush infoForPush = getInfoForPush(gitBranchInfo, entityDetail);
    if (gitBranchInfo.isSyncFromGit()) {
      return SCMNoOpResponse.builder()
          .filePath(gitBranchInfo.getFilePath())
          .pushToDefaultBranch(infoForPush.isDefault())
          .objectId(gitBranchInfo.getLastObjectId())
          .yamlGitConfigId(gitBranchInfo.getYamlGitConfigId())
          .branch(gitBranchInfo.getBranch())
          .folderPath(gitBranchInfo.getFolderPath())
          .build();
    }
    return pushToGitBasedOnChangeType(yaml, changeType, gitBranchInfo, infoForPush);
  }

  private ScmPushResponse pushToGitBasedOnChangeType(
      String yaml, ChangeType changeType, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    if (infoForPush.isNewBranch()) {
      createNewBranchInGit(infoForPush, gitBranchInfo);
    }
    switch (changeType) {
      case ADD:
        final CreateFileResponse createFileResponse = doScmCreateFile(yaml, gitBranchInfo, infoForPush);
        if (createFileResponse.getStatus() == 0) {
          throw new InvalidRequestException("Git push failed");
        }
        return ScmCreateFileResponse.builder()
            .folderPath(infoForPush.getFolderPath())
            .filePath(infoForPush.getFilePath())
            .pushToDefaultBranch(infoForPush.isDefault())
            .yamlGitConfigId(infoForPush.getYamlGitConfigId())
            .accountIdentifier(infoForPush.getAccountId())
            .orgIdentifier(infoForPush.getOrgIdentifier())
            .projectIdentifier(infoForPush.getProjectIdentifier())
            .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
            .branch(infoForPush.getBranch())
            .build();
      case DELETE:
        final DeleteFileResponse deleteFileResponse = doScmDeleteFile(gitBranchInfo, infoForPush);
        if (deleteFileResponse.getStatus() == 0) {
          throw new InvalidRequestException("Git push failed");
        }
        return ScmDeleteFileResponse.builder()
            .accountIdentifier(infoForPush.getAccountId())
            .orgIdentifier(infoForPush.getOrgIdentifier())
            .projectIdentifier(infoForPush.getProjectIdentifier())
            .folderPath(infoForPush.getFolderPath())
            .filePath(infoForPush.getFilePath())
            .pushToDefaultBranch(infoForPush.isDefault())
            .yamlGitConfigId(infoForPush.getYamlGitConfigId())
            .branch(infoForPush.getBranch())
            .build();
      case RENAME:
        throw new NotImplementedException("Not implemented");
      case MODIFY:
        final UpdateFileResponse updateFileResponse = doScmUpdateFile(yaml, gitBranchInfo, infoForPush);
        if (updateFileResponse.getStatus() == 0) {
          throw new InvalidRequestException("Git push failed");
        }
        return ScmUpdateFileResponse.builder()
            .folderPath(infoForPush.getFolderPath())
            .filePath(infoForPush.getFilePath())
            .objectId(EntityObjectIdUtils.getObjectIdOfYaml(yaml))
            .oldObjectId(gitBranchInfo.getLastObjectId())
            .yamlGitConfigId(infoForPush.getYamlGitConfigId())
            .pushToDefaultBranch(infoForPush.isDefault())
            .accountIdentifier(infoForPush.getAccountId())
            .orgIdentifier(infoForPush.getOrgIdentifier())
            .projectIdentifier(infoForPush.getProjectIdentifier())
            .branch(infoForPush.getBranch())
            .build();
      default:
        throw new EnumConstantNotPresentException(changeType.getClass(), "Incorrect changeType");
    }
  }

  private void createNewBranchInGit(InfoForGitPush infoForPush, GitEntityInfo gitBranchInfo) {
    scmClient.createNewBranch(
        infoForPush.getScmConnector(), infoForPush.getBranch(), infoForPush.getDefaultBranchName());
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
            .build());
    if (!pushInfo.getStatus()) {
      if (pushInfo.getException().getValue() == null) {
        throw new InvalidRequestException("Unknown exception occurred");
      }
      throw(WingsException) kryoSerializer.asObject(pushInfo.getException().getValue().toByteArray());
    }
    final ScmConnector scmConnector =
        (ScmConnector) kryoSerializer.asObject(pushInfo.getConnector().getValue().toByteArray());
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
        .build();
  }

  private DeleteFileResponse doScmDeleteFile(GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFilePathDetails gitFilePathDetails =
        GitFilePathDetails.builder().branch(infoForPush.getBranch()).filePath(infoForPush.getFilePath()).build();
    return scmClient.deleteFile(infoForPush.getScmConnector(), gitFilePathDetails);
  }

  private CreateFileResponse doScmCreateFile(String yaml, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFileDetails gitFileDetails = getGitFileDetails(gitBranchInfo, yaml).build();
    return scmClient.createFile(infoForPush.getScmConnector(), gitFileDetails);
  }

  private UpdateFileResponse doScmUpdateFile(String yaml, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFileDetails gitFileDetails =
        getGitFileDetails(gitBranchInfo, yaml).oldFileSha(gitBranchInfo.getLastObjectId()).build();
    return scmClient.updateFile(infoForPush.getScmConnector(), gitFileDetails);
  }

  private GitFileDetailsBuilder getGitFileDetails(GitEntityInfo gitEntityInfo, String yaml) {
    final EmbeddedUser currentUser = ScmUserHelper.getCurrentUser();
    String filePath = createFilePath(gitEntityInfo.getFolderPath(), gitEntityInfo.getFilePath());
    return GitFileDetails.builder()
        .branch(gitEntityInfo.getBranch())
        .commitMessage(
            isEmpty(gitEntityInfo.getCommitMsg()) ? GitSyncConstants.COMMIT_MSG : gitEntityInfo.getCommitMsg())
        .fileContent(yaml)
        .filePath(filePath)
        .userEmail(currentUser.getEmail())
        .userName(currentUser.getName());
  }

  String createFilePath(String folderPath, String filePath) {
    if (isEmpty(folderPath)) {
      throw new InvalidRequestException("Folder path cannot be empty");
    }
    if (isEmpty(filePath)) {
      throw new InvalidRequestException("File path cannot be empty");
    }
    String updatedFolderPath = folderPath.endsWith("/") ? folderPath : folderPath.concat("/");
    String folderPathWithoutStartingSlash =
        updatedFolderPath.charAt(0) != '/' ? updatedFolderPath : updatedFolderPath.substring(1);
    String updatedFilePath = filePath.charAt(0) != '/' ? filePath : filePath.substring(1);
    return folderPathWithoutStartingSlash + updatedFilePath;
  }
}
