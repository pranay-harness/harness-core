package io.harness.gitsync.gitfileactivity.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.GitFileLocationHelper;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.GitFileActivityBuilder;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.Status;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.TriggeredBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivity.GitFileActivityRepository;
import io.harness.gitsync.gitfileactivity.dao.api.repositories.gitFileActivitySummary.GitFileActivitySummaryRepository;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class GitSyncServiceImpl implements GitSyncService {
  @Inject private GitFileActivitySummaryRepository gitFileActivitySummaryRepository;
  @Inject private GitFileActivityRepository gitFileActivityRepository;

  private TriggeredBy getTriggeredBy(boolean isGitToHarness, boolean isFullSync) {
    if (isGitToHarness) {
      return TriggeredBy.GIT;
    } else {
      if (isFullSync) {
        return TriggeredBy.FULL_SYNC;
      } else {
        return TriggeredBy.USER;
      }
    }
  }

  @Override
  public Iterable<GitFileActivity> logActivityForGitOperation(List<GitFileChange> changeList, Status status,
      boolean isGitToHarness, boolean isFullSync, String message, String commitId, String commitMessage,
      YamlGitConfigDTO yamlGitConfig) {
    try {
      if (isEmpty(changeList)) {
        return null;
      }
      final List<GitFileActivity> activities =
          changeList.stream()
              .map(change
                  -> buildBaseGitFileActivity(change, commitId, commitMessage, yamlGitConfig)
                         .status(status)
                         .errorMessage(message)
                         .triggeredBy(getTriggeredBy(isGitToHarness, isFullSync))
                         .build())
              .collect(toList());
      return gitFileActivityRepository.saveAll(activities);
    } catch (Exception ex) {
      throw new InvalidRequestException(format("Error while saving activities: %s", ex));
    }
  }

  private GitFileActivityBuilder buildBaseGitFileActivity(
      GitFileChange change, String commitId, String commitMessage, YamlGitConfigDTO yamlGitConfig) {
    String commitIdToPersist = StringUtils.isEmpty(commitId) ? change.getCommitId() : commitId;
    String processingCommitIdToPersist = StringUtils.isEmpty(commitId) ? change.getProcessingCommitId() : commitId;
    String commitMessageToPersist = StringUtils.isEmpty(commitMessage) ? change.getCommitMessage() : commitMessage;
    String processingCommitMessage =
        StringUtils.isEmpty(commitMessage) ? change.getProcessingCommitMessage() : commitMessage;
    final boolean changeFromAnotherCommit = change.isChangeFromAnotherCommit();
    return GitFileActivity.builder()
        .accountId(change.getAccountId())
        .commitId(commitIdToPersist)
        .processingCommitId(processingCommitIdToPersist)
        .filePath(change.getFilePath())
        .fileContent(change.getFileContent())
        .commitMessage(commitMessageToPersist)
        .processingCommitMessage(processingCommitMessage)
        .changeType(change.getChangeType())
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .repo(yamlGitConfig.getRepo())
        .rootFolder(GitFileLocationHelper.getRootPathSafely(change.getFilePath()))
        .branchName(yamlGitConfig.getBranch())
        .changeFromAnotherCommit(changeFromAnotherCommit);
  }

  private GitFileActivitySummary buildBaseGitFileActivitySummary(
      GitFileActivity gitFileActivity, Boolean gitToHarness, GitCommit.Status status) {
    return GitFileActivitySummary.builder()
        .accountId(gitFileActivity.getAccountId())
        .commitId(gitFileActivity.getCommitId())
        .gitConnectorId(gitFileActivity.getGitConnectorId())
        .repoUrl(gitFileActivity.getRepo())
        .branchName(gitFileActivity.getBranchName())
        .commitMessage(gitFileActivity.getCommitMessage())
        .gitToHarness(gitToHarness)
        .status(status)
        .build();
  }

  private GitFileProcessingSummary createFileProcessingSummary(@NotEmpty List<GitFileActivity> gitFileActivites) {
    final Map<Status, Long> statusToCountMap =
        gitFileActivites.stream()
            .map(gitFileActivity -> gitFileActivity.getStatus())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    final Long totalCount = statusToCountMap.values().stream().reduce(0L, Long::sum);

    return GitFileProcessingSummary.builder()
        .failureCount(statusToCountMap.getOrDefault(Status.FAILED, 0L))
        .successCount(statusToCountMap.getOrDefault(Status.SUCCESS, 0L))
        .skippedCount(statusToCountMap.getOrDefault(Status.SKIPPED, 0L))
        .queuedCount(statusToCountMap.getOrDefault(Status.QUEUED, 0L))
        .totalCount(totalCount)
        .build();
  }

  @Override
  public GitFileActivitySummary createGitFileActivitySummaryForCommit(
      final String commitId, final String accountId, Boolean gitToHarness, GitCommit.Status status) {
    try {
      List<GitFileActivity> gitFileActivities = getFileActivitesForCommit(commitId, accountId);
      if (isEmpty(gitFileActivities)) {
        return null;
      }
      return createGitFileActivitySummary(gitFileActivities, gitToHarness, status);
    } catch (Exception ex) {
      throw new InvalidRequestException(
          format("Error while saving git file processing summary for commitId: %s", commitId), ex);
    }
  }

  @VisibleForTesting
  List<GitFileActivity> getFileActivitesForCommit(String commitId, String accountId) {
    return gitFileActivityRepository.findByAccountIdAndCommitId(accountId, commitId);
  }

  private GitFileActivitySummary createGitFileActivitySummary(
      List<GitFileActivity> gitFileActivities, Boolean gitToHarness, GitCommit.Status status) {
    if (isEmpty(gitFileActivities)) {
      return null;
    }
    GitFileActivity gitFileActivity = gitFileActivities.get(0);
    GitFileActivitySummary gitFileActivitySummary =
        buildBaseGitFileActivitySummary(gitFileActivity, gitToHarness, status);
    gitFileActivitySummary.setFileProcessingSummary(createFileProcessingSummary(gitFileActivities));
    return gitFileActivitySummaryRepository.save(gitFileActivitySummary);
  }
}
