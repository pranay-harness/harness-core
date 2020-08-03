package io.harness.cdng.gitclient;

import static io.harness.delegate.beans.git.GitFileChange.ChangeType.ADD;
import static io.harness.delegate.beans.git.GitFileChange.ChangeType.DELETE;
import static io.harness.delegate.beans.git.GitFileChange.ChangeType.MODIFY;
import static io.harness.eraro.ErrorCode.GIT_CONNECTION_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.delegate.beans.git.GitFileChange;
import io.harness.exception.GeneralException;
import io.harness.filesystem.FileIo;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.TransportException;
import org.jetbrains.annotations.NotNull;
import software.wings.service.impl.yaml.GitConnectionDelegateException;

import java.io.File;
import java.util.Optional;

@Singleton
@Slf4j
public class GitClientHelperNG {
  private static final String GIT_REPO_BASE_DIR = "./repository/${ACCOUNT_ID}/${REPO_NAME}";

  public String getRepoDirectory(GitConfigDTO gitConfig, String accountId) {
    String repoUrl = gitConfig.getGitAuth().getUrl();
    String repoName =
        repoUrl.substring(repoUrl.lastIndexOf('/') + 1).split("\\.")[0]; // TODO:: support more url types and validation

    return GIT_REPO_BASE_DIR.replace("${ACCOUNT_ID}", accountId).replace("${REPO_NAME}", repoName);
  }

  public void checkIfGitConnectivityIssue(Exception ex) {
    // These are the common error we find while delegate runs git command
    // TransportException is subclass of GitAPIException. This is thrown when there is any issue in connecting to git
    // repo, like invalid authorization and invalid repo

    // MissingObjectException is caused when some object(commit/ref) is missing in the git history
    if ((ex instanceof GitAPIException && ex.getCause() instanceof TransportException)
        || ex instanceof JGitInternalException || ex instanceof MissingObjectException) {
      throw new GitConnectionDelegateException(GIT_CONNECTION_ERROR, ex.getCause(), ex.getMessage(), USER_ADMIN);
    }
  }

  public synchronized void releaseLock(GitConfigDTO gitConfig, String repoPath, String accountId) {
    try {
      File repoDir = new File(repoPath);
      File file = new File(repoDir.getAbsolutePath() + "/.git/index.lock");
      FileIo.deleteFileIfExists(file.getAbsolutePath());
    } catch (Exception e) {
      logger.error(new StringBuilder(64)
                       .append("Failed to delete index.lock file for account: ")
                       .append(accountId)
                       .append(", Repo URL: ")
                       .append(gitConfig.getGitAuth().getUrl())
                       .append(", Branch: ")
                       .append(gitConfig.getGitAuth().getBranchName())
                       .toString());

      throw new GeneralException("GIT_SYNC_ISSUE: Failed to delete index.lock file", e, SRE);
    }
  }

  @NotNull
  public StringBuilder prepareCommitMessage(GitConfigDTO gitConfig, Status status, String accountId) {
    StringBuilder commitMessage = new StringBuilder(48);
    Optional<String> msg = Optional.ofNullable(gitConfig.getGitSyncConfig())
                               .map(GitSyncConfig::getCustomCommitAttributes)
                               .map(CustomCommitAttributes::getCommitMessage);
    if (msg.isPresent() && isNotBlank(msg.get())) {
      commitMessage.append(gitConfig.getGitSyncConfig().getCustomCommitAttributes().getCommitMessage());
    } else {
      commitMessage.append("Harness IO Git Sync. \n");
      status.getAdded().forEach(
          filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.ADD, filePath)));
      status.getChanged().forEach(
          filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.MODIFY, filePath)));
      status.getRemoved().forEach(
          filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.DELETE, filePath)));
    }
    logger.info(format("Commit message for git sync for accountId: [%s]  repoUrl: [%s] %n "
            + "Additions: [%d] Modifications:[%d] Deletions:[%d]"
            + "%n Message: [%s]",
        accountId, gitConfig.getGitAuth().getUrl(), status.getAdded().size(), status.getChanged().size(),
        status.getRemoved().size(), commitMessage));
    return commitMessage;
  }

  public String getTruncatedCommitMessage(String commitMessage) {
    if (isBlank(commitMessage)) {
      return commitMessage;
    }
    return commitMessage.substring(0, Math.min(commitMessage.length(), 500));
  }

  public GitFileChange.ChangeType getChangeType(DiffEntry.ChangeType gitDiffChangeType) {
    switch (gitDiffChangeType) {
      case ADD:
        return ADD;
      case MODIFY:
        return MODIFY;
      case DELETE:
        return DELETE;
      default:
        unhandled(gitDiffChangeType);
    }
    return null;
  }
}
