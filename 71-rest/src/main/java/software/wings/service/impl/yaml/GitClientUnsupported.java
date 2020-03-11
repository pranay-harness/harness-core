package software.wings.service.impl.yaml;

import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCloneResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
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
  public void ensureRepoLocallyClonedAndUpdated(GitOperationContext gitOperationContext) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitDiffResult diff(GitOperationContext gitOperationContext, boolean excludeFilesOutsideSetupFolder) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCheckoutResult checkout(GitOperationContext gitOperationContext) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCommitResult commit(GitOperationContext gitOperationContext) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitPushResult push(GitOperationContext gitOperationContext) {
    throw new UnsupportedOperationException("Git operations not supported.");
  }

  @Override
  public GitCommitAndPushResult commitAndPush(GitOperationContext gitOperationContext) {
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
