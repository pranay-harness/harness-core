package io.harness.gitsync.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.impl.ScmResponseStatusUtils;
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
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            createFileResponse.getStatus(), createFileResponse.getError());
        return ScmGitUtils.createScmCreateFileResponse(yaml, infoForPush);
      case DELETE:
        final DeleteFileResponse deleteFileResponse = doScmDeleteFile(gitBranchInfo, infoForPush);
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            deleteFileResponse.getStatus(), deleteFileResponse.getError());
        return ScmGitUtils.createScmDeleteFileResponse(yaml, infoForPush);
      case RENAME:
        throw new NotImplementedException("Not implemented");
      case MODIFY:
        final UpdateFileResponse updateFileResponse = doScmUpdateFile(yaml, gitBranchInfo, infoForPush);
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
            updateFileResponse.getStatus(), updateFileResponse.getError());
        return ScmGitUtils.createScmUpdateFileResponse(yaml, infoForPush);
      default:
        throw new EnumConstantNotPresentException(changeType.getClass(), "Incorrect changeType");
    }
  }

  private void createNewBranchInGit(InfoForGitPush infoForPush, GitEntityInfo gitBranchInfo) {
    scmClient.createNewBranch(infoForPush.getScmConnector(), infoForPush.getBranch(), gitBranchInfo.getBaseBranch());
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
