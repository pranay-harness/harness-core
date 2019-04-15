package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFilesBetweenCommitsRequest;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class GitServiceImpl implements GitService {
  @Inject private GitClient gitClient;

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, String connectorId, String commitId, String branch,
      List<String> filePaths, boolean useBranch) {
    return gitClient.fetchFilesByPath(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(commitId)
            .branch(branch)
            .filePaths(filePaths)
            .gitConnectorId(connectorId)
            .useBranch(useBranch)
            .recursive(true)
            .build());
  }

  @Override
  public GitFetchFilesResult fetchFilesBetweenCommits(
      GitConfig gitConfig, String newCommitId, String oldCommitId, String connectorId) {
    return gitClient.fetchFilesBetweenCommits(gitConfig,
        GitFilesBetweenCommitsRequest.builder()
            .newCommitId(newCommitId)
            .oldCommitId(oldCommitId)
            .gitConnectorId(connectorId)
            .build());
  }

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, String connectorId, String commitId, String branch,
      List<String> filePaths, boolean useBranch, List<String> fileExtensions, boolean isRecursive) {
    return gitClient.fetchFilesByPath(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(commitId)
            .branch(branch)
            .filePaths(filePaths)
            .gitConnectorId(connectorId)
            .useBranch(useBranch)
            .fileExtensions(fileExtensions)
            .recursive(isRecursive)
            .build());
  }

  @Override
  public void checkoutFilesByPathForHelmSourceRepo(GitConfig gitConfig, String connectorId, String commitId,
      String branch, List<String> filePaths, boolean useBranch) {
    gitClient.checkoutFilesByPathForHelmSourceRepo(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(commitId)
            .branch(branch)
            .filePaths(filePaths)
            .gitConnectorId(connectorId)
            .useBranch(useBranch)
            .recursive(true)
            .build());
  }

  @Override
  public void resetWorkingDir(GitConfig gitConfig, String gitConnectorId) {
    gitClient.resetWorkingDir(gitConfig, gitConnectorId);
  }
}
