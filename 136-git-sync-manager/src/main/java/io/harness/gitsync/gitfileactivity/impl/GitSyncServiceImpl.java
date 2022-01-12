/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitfileactivity.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.previousOperation;
import static org.springframework.data.mongodb.core.aggregation.Fields.UNDERSCORE_ID;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.helper.GitFileLocationHelper;
import io.harness.gitsync.core.beans.ChangeWithErrorMsg;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.GitFileActivityBuilder;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.GitFileActivityKeys;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.Status;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity.TriggeredBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary.GitFileActivitySummaryBuilder;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.repositories.gitFileActivity.GitFileActivityRepository;
import io.harness.repositories.gitFileActivitySummary.GitFileActivitySummaryRepository;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(DX)
public class GitSyncServiceImpl implements GitSyncService {
  @Inject private GitFileActivitySummaryRepository gitFileActivitySummaryRepository;
  @Inject private GitFileActivityRepository gitFileActivityRepository;

  @Override
  public List<GitFileActivity> saveAll(List<GitFileActivity> gitFileActivities) {
    List<GitFileActivity> gitFileActivitiesSaved = new ArrayList<>();
    gitFileActivityRepository.saveAll(gitFileActivities).iterator().forEachRemaining(gitFileActivitiesSaved::add);
    return gitFileActivitiesSaved;
  }

  @Override
  public void updateGitFileActivity(List<ChangeWithErrorMsg> failedYamlFileChangeMap,
      List<GitFileChange> successfulGitFileChanges, List<GitFileChange> skippedGitFileChanges, String accountId,
      boolean isFullSync, String commitMessage, YamlGitConfigDTO yamlGitConfig) {
    logActivitiesForFailedChanges(failedYamlFileChangeMap, accountId, isFullSync, commitMessage, yamlGitConfig);
    logActivityForSuccessfulFiles(successfulGitFileChanges, accountId, commitMessage, yamlGitConfig);
    markActivityWithSkippedFiles(skippedGitFileChanges, commitMessage, accountId);
  }

  @Override
  public void logActivitiesForFailedChanges(List<ChangeWithErrorMsg> failedYamlFileChangeMap, String accountId,
      boolean isFullSync, String commitMessage, YamlGitConfigDTO yamlGitConfig) {
    if (failedYamlFileChangeMap == null) {
      return;
    }
    List<ChangeWithErrorMsg> failuresWhichArePartOfCommit = new ArrayList<>();
    List<ChangeWithErrorMsg> extraFilesFailedWhileProcessing = new ArrayList<>();
    failedYamlFileChangeMap.forEach(changeDetails -> {
      if (isChangePartOfCommit(changeDetails)) {
        failuresWhichArePartOfCommit.add(changeDetails);
      } else {
        extraFilesFailedWhileProcessing.add(changeDetails);
      }
    });
    addActivityForExtraErrorsIfMessageChanged(
        extraFilesFailedWhileProcessing, isFullSync, commitMessage, accountId, yamlGitConfig);
    updateStatusOnProcessingFailure(failuresWhichArePartOfCommit, accountId, yamlGitConfig);
  }

  private boolean isChangePartOfCommit(ChangeWithErrorMsg changeWithErrorMsg) {
    return !(changeWithErrorMsg.getChange()).isChangeFromAnotherCommit();
  }

  private void updateStatusOnProcessingFailure(
      List<ChangeWithErrorMsg> changeWithErrorMsgs, String accountId, YamlGitConfigDTO yamlGitConfig) {
    if (isEmpty(changeWithErrorMsgs)) {
      return;
    }
    changeWithErrorMsgs.forEach(changeWithErrorMsg -> {
      GitFileChange change = changeWithErrorMsg.getChange();
      if (isChangeFromGit(change)) {
        updateStatusOfGitFileActivity(change.getProcessingCommitId(), Collections.singletonList(change.getFilePath()),
            Status.FAILED, changeWithErrorMsg.getErrorMsg(), accountId);
      }
    });
  }

  private void addActivityForExtraErrorsIfMessageChanged(List<ChangeWithErrorMsg> changesFailed, boolean isFullSync,
      String commitMessage, String accountId, YamlGitConfigDTO yamlGitConfig) {
    if (isEmpty(changesFailed)) {
      return;
    }
    List<String> nameOfFilesProcessedInCommit = getNameOfFilesProcessed(changesFailed);
    Map<String, String> latestActivitiesForFiles = getLatestActivitiesForFiles(
        nameOfFilesProcessedInCommit, yamlGitConfig.getRepo(), yamlGitConfig.getBranch(), accountId);
    changesFailed.forEach(failedChange
        -> createFileActivityIfErrorChanged(
            failedChange, latestActivitiesForFiles, isFullSync, commitMessage, yamlGitConfig));
  }

  private void createFileActivityIfErrorChanged(ChangeWithErrorMsg changeWithErrorMsg,
      Map<String, String> latestActivitiesForFiles, boolean isFullSync, String commitMessage,
      YamlGitConfigDTO yamlGitConfig) {
    String newErrorMessage = changeWithErrorMsg.getErrorMsg();
    GitFileChange change = changeWithErrorMsg.getChange();
    String filePath = change.getFilePath();
    if (latestActivitiesForFiles.containsKey(filePath)) {
      if (!StringUtils.defaultIfBlank(latestActivitiesForFiles.get(filePath), "").equals(newErrorMessage)) {
        GitFileActivity newFileActivity =
            createGitFileActivityForFailedExtraFile(change, newErrorMessage, isFullSync, yamlGitConfig);
        gitFileActivityRepository.save(newFileActivity);
      }
    } else {
      log.info(
          "Unexpected Behaviour while processing extra error in commit: No file activity found for file {}", filePath);
    }
  }

  private GitFileActivity createGitFileActivityForFailedExtraFile(
      GitFileChange change, String errorMessage, boolean isFullSync, YamlGitConfigDTO yamlGitConfig) {
    return buildBaseGitFileActivity(change, "", "", yamlGitConfig)
        .status(Status.FAILED)
        .errorMessage(errorMessage)
        .triggeredBy(getTriggeredBy(change.isSyncFromGit(), isFullSync))
        .commitMessage(change.getCommitMessage())
        .build();
  }

  private List<String> getNameOfFilesProcessed(List<ChangeWithErrorMsg> changeWithErrorMsgs) {
    if (isEmpty(changeWithErrorMsgs)) {
      return Collections.emptyList();
    }
    return changeWithErrorMsgs.stream()
        .map(changeWithErrorMsg -> changeWithErrorMsg.getChange().getFilePath())
        .collect(Collectors.toList());
  }

  private Map<String, String> getLatestActivitiesForFiles(
      List<String> filePaths, String repo, String branch, String accountId) {
    Map<String, String> fileNameErrorMap = new HashMap<>();
    Criteria criteria = Criteria.where(GitFileActivityKeys.accountId)
                            .is(accountId)
                            .and(GitFileActivityKeys.filePath)
                            .in(filePaths)
                            .and(GitFileActivityKeys.repo)
                            .is(repo)
                            .and(GitFileActivityKeys.branchName)
                            .is(branch);

    Aggregation aggregation = newAggregation(Aggregation.match(criteria),
        Aggregation.sort(Sort.Direction.DESC, GitFileActivityKeys.createdAt),
        Aggregation.group(GitFileActivityKeys.filePath)
            .first(GitFileActivityKeys.errorMessage)
            .as(GitFileActivityKeys.errorMessage),
        Aggregation.project()
            .andExpression(UNDERSCORE_ID, previousOperation())
            .as(GitFileActivityKeys.filePath)
            .andExpression(GitFileActivityKeys.errorMessage)
            .as(GitFileActivityKeys.errorMessage));

    final List<LatestErrorInGitFileActivity> latestErrorFileMap =
        gitFileActivityRepository.aggregate(aggregation, LatestErrorInGitFileActivity.class).getMappedResults();
    latestErrorFileMap.forEach(
        fileErrorPair -> fileNameErrorMap.put(fileErrorPair.getFilePath(), fileErrorPair.getErrorMessage()));

    return fileNameErrorMap;
  }

  public boolean isChangeFromGit(GitFileChange change) {
    try {
      return change.isSyncFromGit() && isNotEmpty(change.getProcessingCommitId());
    } catch (Exception ex) {
      log.error(format("Error while checking if change is from git: %s", ex));
    }
    return false;
  }

  @Override
  public void logActivityForSuccessfulFiles(
      List<GitFileChange> gitFileChanges, String accountId, String commitMessage, YamlGitConfigDTO yamlGitConfig) {
    if (isEmpty(gitFileChanges)) {
      return;
    }
    gitFileChanges.forEach(
        gitFileChange -> onGitFileProcessingSuccess(gitFileChange, accountId, commitMessage, yamlGitConfig));
  }

  @Override
  public void onGitFileProcessingSuccess(
      GitFileChange change, String accountId, String commitMessage, YamlGitConfigDTO yamlGitConfig) {
    if (isChangeFromGit(change)) {
      if (change.isChangeFromAnotherCommit()) {
        logActivityForGitOperation(singletonList(change), Status.SUCCESS, true, false, null,
            change.getProcessingCommitId(), commitMessage, yamlGitConfig);
      } else {
        updateStatusOfGitFileActivity(
            change.getProcessingCommitId(), singletonList(change.getFilePath()), Status.SUCCESS, null, accountId);
      }
    }
  }

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
      boolean isGitToHarness, boolean isFullSync, String errorMessage, String commitId, String commitMessage,
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
                         .errorMessage(errorMessage)
                         .triggeredBy(getTriggeredBy(isGitToHarness, isFullSync))
                         .build())
              .collect(toList());
      return gitFileActivityRepository.saveAll(activities);
    } catch (Exception ex) {
      throw new InvalidRequestException(format("Error while saving activities: %s", ex));
    }
  }

  @Override
  public void updateStatusOfGitFileActivity(
      final String commitId, final List<String> fileNames, Status status, String errorMessage, String accountId) {
    if (EmptyPredicate.isEmpty(fileNames)) {
      return;
    }
    try {
      gitFileActivityRepository.updateGitFileActivityStatus(status, errorMessage, accountId, commitId, fileNames, null);
    } catch (Exception ex) {
      log.error(format("Error while saving activities for commitId: %s", "commitId"), ex);
    }
  }

  @Override
  public void logActivityForSkippedFiles(List<GitFileChange> changeList, List<GitFileChange> completeChangeList,
      String message, String accountId, String commitId) {
    List<GitFileChange> skippedChangeList = ListUtils.removeAll(completeChangeList, changeList);
    markActivityWithSkippedFiles(skippedChangeList, message, accountId);
  }

  public void markActivityWithSkippedFiles(List<GitFileChange> skippedChangeList, String message, String accountId) {
    if (isEmpty(skippedChangeList)) {
      return;
    }
    updateStatusOfGitFileActivity(skippedChangeList.get(0).getProcessingCommitId(),
        skippedChangeList.stream().map(GitFileChange::getFilePath).collect(Collectors.toList()), Status.SKIPPED,
        message, accountId);
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
        .gitConnectorId(yamlGitConfig.getGitConnectorRef())
        .repo(yamlGitConfig.getRepo())
        .rootFolder(GitFileLocationHelper.getRootPathSafely(change.getFilePath()))
        .branchName(yamlGitConfig.getBranch())
        .changeFromAnotherCommit(changeFromAnotherCommit);
  }

  private GitFileActivitySummaryBuilder buildBaseGitFileActivitySummary(
      GitFileActivity gitFileActivity, boolean gitToHarness, GitCommitProcessingStatus status) {
    return GitFileActivitySummary.builder()
        .accountId(gitFileActivity.getAccountId())
        .organizationId(gitFileActivity.getOrganizationId())
        .projectId(gitFileActivity.getProjectId())
        .commitId(gitFileActivity.getCommitId())
        .gitConnectorId(gitFileActivity.getGitConnectorId())
        .repo(gitFileActivity.getRepo())
        .branchName(gitFileActivity.getBranchName())
        .commitMessage(gitFileActivity.getCommitMessage())
        .gitToHarness(gitToHarness)
        .status(status);
  }

  private GitFileActivitySummaryBuilder buildBaseGitFileActivitySummary(GitCommit gitCommit, boolean gitToHarness) {
    return GitFileActivitySummary.builder()
        .accountId(gitCommit.getAccountIdentifier())
        //        .organizationId(gitCommit.getOrganizationId())
        //        .projectId(gitCommit.getProjectId())
        .commitId(gitCommit.getCommitId())
        .branchName(gitCommit.getBranchName())
        .repo(gitCommit.getRepoURL())
        .commitMessage(gitCommit.getCommitMessage())
        .gitToHarness(gitToHarness)
        .status(gitCommit.getStatus());
  }

  private GitFileProcessingSummary createFileProcessingSummary(@NotEmpty List<GitFileActivity> gitFileActivites) {
    final Map<Status, Long> statusToCountMap =
        gitFileActivites.stream()
            .map(GitFileActivity::getStatus)
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
  public GitFileActivitySummary createGitFileActivitySummaryForCommit(final String commitId, final String accountId,
      Boolean gitToHarness, GitCommitProcessingStatus status, YamlGitConfigDTO yamlGitConfig) {
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
      List<GitFileActivity> gitFileActivities, Boolean gitToHarness, GitCommitProcessingStatus status) {
    if (isEmpty(gitFileActivities)) {
      return null;
    }
    GitFileActivity gitFileActivity = gitFileActivities.get(0);
    GitFileActivitySummary gitFileActivitySummary =
        buildBaseGitFileActivitySummary(gitFileActivity, gitToHarness, status).build();
    gitFileActivitySummary.setFileProcessingSummary(createFileProcessingSummary(gitFileActivities));
    return gitFileActivitySummaryRepository.save(gitFileActivitySummary);
  }

  public void createGitFileSummaryForFailedOrSkippedCommit(GitCommit gitCommit, boolean gitToHarness) {
    List<GitFileActivitySummary> gitFileActivitySummary = new ArrayList<>();
    gitFileActivitySummary.add(getGitFileActivitySummary(gitCommit, gitToHarness));
    gitFileActivitySummaryRepository.saveAll(gitFileActivitySummary);
  }

  private GitFileActivitySummary getGitFileActivitySummary(GitCommit gitCommit, boolean gitToHarness) {
    return buildBaseGitFileActivitySummary(gitCommit, gitToHarness).build();
  }
}
