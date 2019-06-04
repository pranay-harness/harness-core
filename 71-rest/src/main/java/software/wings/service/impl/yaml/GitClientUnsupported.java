package software.wings.service.impl.yaml;

import org.eclipse.jgit.api.PullResult;
import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCloneResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitCommitResult;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFilesBetweenCommitsRequest;
import software.wings.beans.yaml.GitPushResult;
import software.wings.service.intfc.yaml.GitClient;

public class GitClientUnsupported implements GitClient {
  @Override
  public GitCloneResult clone(GitConfig gitConfig, String gitRepoDirectory, String branch, boolean noCheckout) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public void ensureRepoLocallyClonedAndUpdated(GitConfig gitConfig) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitDiffResult diff(GitConfig gitConfig, String startCommitId) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCheckoutResult checkout(GitConfig gitConfig) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCommitResult commit(GitConfig gitConfig, GitCommitRequest gitCommitRequest) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitPushResult push(GitConfig gitConfig, boolean forcePush) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCommitAndPushResult commitAndPush(GitConfig gitConfig, GitCommitRequest gitCommitRequest) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public PullResult pull(GitConfig gitConfig) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public String validate(GitConfig gitConfig) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, GitFetchFilesRequest gitRequest) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitFetchFilesResult fetchFilesBetweenCommits(GitConfig gitConfig, GitFilesBetweenCommitsRequest gitRequest) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public void checkoutFilesByPathForHelmSourceRepo(GitConfig gitConfig, GitFetchFilesRequest gitRequest) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public void resetWorkingDir(GitConfig gitConfig, String gitConnectorId) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public void downloadFiles(GitConfig gitConfig, GitFetchFilesRequest gitRequest, String destinationDirectory) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }
}
