package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class GitServiceImpl implements GitService {
  private static final Logger logger = LoggerFactory.getLogger(GitServiceImpl.class);

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
}
