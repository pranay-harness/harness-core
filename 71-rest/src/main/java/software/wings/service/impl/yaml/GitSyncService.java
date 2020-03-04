package software.wings.service.impl.yaml;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.Application;
import software.wings.beans.GitCommit;
import software.wings.beans.yaml.Change;
import software.wings.exception.YamlProcessingException;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileProcessingSummary;

import java.util.List;
import java.util.Map;

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
  PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req);

  /**
   *
   * @param errors
   * @param status
   * @param accountId
   */
  void updateGitSyncErrorStatus(List<GitSyncError> errors, Status status, String accountId);

  /**
   *
   * @param accountId
   * @return
   */
  List<Application> fetchRepositories(String accountId);

  /**
   *
   * @param req
   * @param accountId
   * @return
   */
  PageResponse<GitCommit> fetchGitCommits(PageRequest<GitCommit> req, String accountId);

  /**
   *
   * @param changeList
   * @param failedYamlFileChangeMap
   * @param status
   * @param errorMessage
   */
  GitFileProcessingSummary logFileActivityAndGenerateProcessingSummary(List<Change> changeList,
      Map<String, YamlProcessingException.ChangeWithErrorMsg> failedYamlFileChangeMap, Status status,
      String errorMessage);
}
