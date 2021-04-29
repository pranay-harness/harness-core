package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmCreateFileResponse;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@Singleton
@OwnedBy(DX)
public class ScmManagerGitHelper implements ScmGitHelper {
  @Inject private ScmClient scmClient;

  @Override
  public ScmPushResponse pushToGitBasedOnChangeType(
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
        return ScmGitUtils.createScmCreateFileResponse(yaml, infoForPush);
      case DELETE:
        final DeleteFileResponse deleteFileResponse = doScmDeleteFile(gitBranchInfo, infoForPush);
        if (deleteFileResponse.getStatus() == 0) {
          throw new InvalidRequestException("Git push failed");
        }
        return ScmGitUtils.createScmDeleteFileResponse(yaml, infoForPush);
      case RENAME:
        throw new NotImplementedException("Not implemented");
      case MODIFY:
        final UpdateFileResponse updateFileResponse = doScmUpdateFile(yaml, gitBranchInfo, infoForPush);
        if (updateFileResponse.getStatus() == 0) {
          throw new InvalidRequestException("Git push failed");
        }
        return ScmGitUtils.createScmUpdateFileResponse(yaml, infoForPush);
      default:
        throw new EnumConstantNotPresentException(changeType.getClass(), "Incorrect changeType");
    }
  }

  private ScmCreateFileResponse getBuild(String yaml, InfoForGitPush infoForPush) {
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
  }

  private void createNewBranchInGit(InfoForGitPush infoForPush, GitEntityInfo gitBranchInfo) {
    scmClient.createNewBranch(
        infoForPush.getScmConnector(), infoForPush.getBranch(), infoForPush.getDefaultBranchName());
  }

  private DeleteFileResponse doScmDeleteFile(GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFilePathDetails gitFilePathDetails =
        GitFilePathDetails.builder().branch(infoForPush.getBranch()).filePath(infoForPush.getFilePath()).build();
    return scmClient.deleteFile(infoForPush.getScmConnector(), gitFilePathDetails);
  }

  private CreateFileResponse doScmCreateFile(String yaml, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFileDetails gitFileDetails = ScmGitUtils.getGitFileDetails(gitBranchInfo, yaml).build();
    return scmClient.createFile(infoForPush.getScmConnector(), gitFileDetails);
  }

  private UpdateFileResponse doScmUpdateFile(String yaml, GitEntityInfo gitBranchInfo, InfoForGitPush infoForPush) {
    final GitFileDetails gitFileDetails =
        ScmGitUtils.getGitFileDetails(gitBranchInfo, yaml).oldFileSha(gitBranchInfo.getLastObjectId()).build();
    return scmClient.updateFile(infoForPush.getScmConnector(), gitFileDetails);
  }
}
