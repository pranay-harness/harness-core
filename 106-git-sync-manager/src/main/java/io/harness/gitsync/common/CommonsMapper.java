package io.harness.gitsync.common;

import static io.harness.ng.core.gitsync.ChangeType.ADD;
import static io.harness.ng.core.gitsync.ChangeType.DELETE;
import static io.harness.ng.core.gitsync.ChangeType.MODIFY;

import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.ng.core.gitsync.ChangeType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonsMapper {
  public static GitFileChange toCoreGitFileChange(io.harness.git.model.GitFileChange gitFileChange) {
    return GitFileChange.builder()
        .rootPathId(gitFileChange.getRootPathId())
        .rootPath(gitFileChange.getRootPath())
        .syncFromGit(gitFileChange.isSyncFromGit())
        .oldFilePath(gitFileChange.getOldFilePath())
        .filePath(gitFileChange.getFilePath())
        .fileContent(gitFileChange.getFileContent())
        .commitTimeMs(gitFileChange.getCommitTimeMs())
        .accountId(gitFileChange.getAccountId())
        .commitId(gitFileChange.getCommitId())
        .changeFromAnotherCommit(gitFileChange.isChangeFromAnotherCommit())
        .objectId(gitFileChange.getObjectId())
        .commitMessage(gitFileChange.getCommitMessage())
        .processingCommitId(gitFileChange.getProcessingCommitId())
        .processingCommitMessage(gitFileChange.getProcessingCommitMessage())
        .processingCommitTimeMs(gitFileChange.getProcessingCommitTimeMs())
        .changeType(castChangeTypeToGitSyncChangeType(gitFileChange.getChangeType()))
        .build();
  }

  public static io.harness.git.model.GitFileChange toDelegateGitFileChange(GitFileChange gitFileChange) {
    return io.harness.git.model.GitFileChange.builder()
        .accountId(gitFileChange.getAccountId())
        .changeFromAnotherCommit(gitFileChange.isChangeFromAnotherCommit())
        .changeType(castChangeTypeToDelegateChangeType(gitFileChange.getChangeType()))
        .commitId(gitFileChange.getCommitId())
        .commitMessage(gitFileChange.getCommitMessage())
        .fileContent(gitFileChange.getFileContent())
        .commitTimeMs(gitFileChange.getCommitTimeMs())
        .filePath(gitFileChange.getFilePath())
        .oldFilePath(gitFileChange.getOldFilePath())
        .processingCommitId(gitFileChange.getProcessingCommitId())
        .objectId(gitFileChange.getObjectId())
        .processingCommitMessage(gitFileChange.getProcessingCommitMessage())
        .processingCommitTimeMs(gitFileChange.getProcessingCommitTimeMs())
        .rootPath(gitFileChange.getRootPath())
        .rootPathId(gitFileChange.getRootPathId())
        .syncFromGit(gitFileChange.isSyncFromGit())
        .build();
  }

  private static io.harness.git.model.ChangeType castChangeTypeToDelegateChangeType(ChangeType changeType) {
    switch (changeType) {
      case DELETE:
        return io.harness.git.model.ChangeType.DELETE;
      case MODIFY:
        return io.harness.git.model.ChangeType.MODIFY;
      case ADD:
        return io.harness.git.model.ChangeType.ADD;
      default:
        throw new InvalidRequestException("Invalid changetype");
    }
  }

  private static ChangeType castChangeTypeToGitSyncChangeType(io.harness.git.model.ChangeType changeType) {
    switch (changeType) {
      case DELETE:
        return DELETE;
      case MODIFY:
        return MODIFY;
      case ADD:
        return ADD;
      default:
        throw new InvalidRequestException("Invalid changetype");
    }
  }
}
