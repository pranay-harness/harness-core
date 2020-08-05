package software.wings.service.intfc.yaml;

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

/**
 * Created by anubhaw on 10/16/17.
 */

/**
 * The interface Git client.
 */
public interface GitClient {
  /**
   * Clone git clone result.
   *
   * @param gitConfig the git config
   * @return the git clone result
   */
  GitCloneResult clone(GitConfig gitConfig, String gitRepoDirectory, String branch, boolean noCheckout);

  void ensureRepoLocallyClonedAndUpdated(GitOperationContext gitOperationContext);

  GitDiffResult diff(GitOperationContext gitOperationContext, boolean excludeFilesOutsideSetupFolder);

  GitCheckoutResult checkout(GitOperationContext gitOperationContext);

  GitCommitResult commit(GitOperationContext gitOperationContext);

  GitPushResult push(GitOperationContext gitOperationContext);

  GitCommitAndPushResult commitAndPush(GitOperationContext gitOperationContext);

  String validate(GitConfig gitConfig);

  GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, GitFetchFilesRequest gitRequest);

  GitFetchFilesResult fetchFilesBetweenCommits(GitConfig gitConfig, GitFilesBetweenCommitsRequest gitRequest);

  void checkoutFilesByPathForHelmSourceRepo(GitConfig gitConfig, GitFetchFilesRequest gitRequest);

  void resetWorkingDir(GitConfig gitConfig, String gitConnectorId);

  void downloadFiles(GitConfig gitConfig, GitFetchFilesRequest gitRequest, String destinationDirectory);
}
