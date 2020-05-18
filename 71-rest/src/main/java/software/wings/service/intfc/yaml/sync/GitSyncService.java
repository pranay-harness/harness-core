package software.wings.service.intfc.yaml.sync;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.GitCommit;
import software.wings.beans.GitDetail;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.YamlProcessingException;
import software.wings.service.impl.yaml.gitsync.ChangeSetDTO;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GitSyncService {
  /**
   *
   * @param accountId
   * @return
   */
  List<GitDetail> fetchRepositoriesAccessibleToUser(String accountId);

  /**
   *
   * @param req
   * @param accountId
   * @return
   */
  PageResponse<GitCommit> fetchGitCommits(PageRequest<GitCommit> req, Boolean gitToHarness, String accountId);

  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req, String accountId);

  /**
   *
   * @param changeList
   * @param status
   * @param isGitToHarness
   * @param isFullSync
   * @param message
   */
  void logActivityForGitOperation(List<GitFileChange> changeList, Status status, boolean isGitToHarness,
      boolean isFullSync, String message, String commitId, String commitMessage);

  /**
   *
   * @param commitId
   * @param fileNames
   * @param status
   * @param message
   * @param accountId
   */
  void updateStatusOfGitFileActivity(
      String commitId, List<String> fileNames, Status status, String message, String accountId);

  /**
   *
   * @param validChangeList
   * @param gitDiffResult
   * @param message
   * @param accountId
   */
  void logActivityForSkippedFiles(
      List<GitFileChange> validChangeList, GitDiffResult gitDiffResult, String message, String accountId);

  /**q
   *  @param commitId
   * @param accountId
   */
  void createGitFileActivitySummaryForCommit(
      String commitId, String accountId, Boolean gitToHarness, GitCommit.Status status);

  /**
   * @param commitId
   * @param accountId
   */
  void markRemainingFilesAsSkipped(String commitId, String accountId);

  List<GitFileActivity> getActivitiesForGitSyncErrors(List<GitSyncError> errors, Status status);

  void logActivitiesForFailedChanges(Map<String, YamlProcessingException.ChangeWithErrorMsg> failedYamlFileChangeMap,
      String accountId, boolean isFullSync, String commitMessage);

  void onGitFileProcessingSuccess(Change change, String accountId);

  void createGitFileSummaryForFailedOrSkippedCommit(GitCommit gitCommit, boolean gitToHarness);

  List<ChangeSetDTO> getCommitsWhichAreBeingProcessed(String accountId, String appId, int count, Boolean gitToHarness);

  void changeAppIdOfNewlyAddedFiles(Set<String> nameOfTheNewAppsAdded, String accountId, String processingCommitId);
}
