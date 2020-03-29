package software.wings.service.intfc.yaml.sync;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.GitCommit;
import software.wings.beans.GitDetail;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.GitToHarnessErrorCommitStats;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.List;

public interface GitSyncService {
  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitSyncError> fetchErrors(PageRequest<GitSyncError> req);

  /**
   *
   * @param req
   * @return
   */

  /**
   *
   * @param errors
   * @param status
   * @param accountId
   */
  void deleteGitSyncErrorAndLogFileActivity(List<GitSyncError> errors, Status status, String accountId);

  /**
   *
   * @param accountId
   * @return
   */
  List<GitDetail> fetchRepositories(String accountId);

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
  PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req);

  /**
   *
   * @param changeList
   * @param status
   * @param isGitToHarness
   * @param isFullSync
   * @param message
   */
  void logActivityForGitOperation(List<GitFileChange> changeList, Status status, boolean isGitToHarness,
      boolean isFullSync, String message, String commitId);

  /**
   *
   * @param commitId
   * @param fileNames
   * @param status
   * @param message
   * @param accountId
   */
  void logActivityForFiles(String commitId, List<String> fileNames, Status status, String message, String accountId);

  /**
   *
   * @param validChangeList
   * @param gitDiffResult
   * @param message
   * @param accountId
   */
  void logActivityForSkippedFiles(
      List<GitFileChange> validChangeList, GitDiffResult gitDiffResult, String message, String accountId);

  /**
   *
   * @param commitId
   * @param accountId
   * @param gitFileChanges
   */
  void addFileProcessingSummaryToGitCommit(String commitId, String accountId, List<GitFileChange> gitFileChanges);

  /**
   *
   * @param commitId
   * @param accountId
   */
  void markRemainingFilesAsSkipped(String commitId, String accountId);

  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitToHarnessErrorCommitStats> fetchGitToHarnessErrors(
      PageRequest<GitToHarnessErrorCommitStats> req, String accountId, String yamlGitConfigId);
  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitSyncError> fetchErrorsInEachCommits(
      PageRequest<GitSyncError> req, String gitCommitId, String accountId);

  /**
   *
   * @param req
   * @return
   */
  PageResponse<GitProcessingError> fetchGitProcessingErrors(PageRequest<GitProcessingError> req, String accountId);
}
