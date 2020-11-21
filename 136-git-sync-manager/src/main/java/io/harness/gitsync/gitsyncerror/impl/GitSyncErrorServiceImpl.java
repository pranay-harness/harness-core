package io.harness.gitsync.gitsyncerror.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncDirection.GIT_TO_HARNESS;
import static io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncDirection.HARNESS_TO_GIT;
import static io.harness.gitsync.gitsyncerror.utils.GitSyncErrorUtils.getCommitIdOfError;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.helper.GitFileLocationHelper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.YamlGitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails;
import io.harness.gitsync.gitsyncerror.dao.api.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncErrorServiceImpl implements GitSyncErrorService {
  public static final String EMPTY_STR = "";
  public static final Long DEFAULT_COMMIT_TIME = 0L;

  private static final EnumSet<GitFileActivity.Status> TERMINATING_STATUSES =
      EnumSet.of(GitFileActivity.Status.EXPIRED, GitFileActivity.Status.DISCARDED);
  private YamlGitService yamlGitService;
  private GitSyncService gitSyncService;
  private GitSyncErrorRepository gitSyncErrorRepository;
  private YamlGitConfigService yamlGitConfigService;

  @Override
  public void deleteByAccountIdOrgIdProjectIdAndFilePath(
      String accountId, String orgId, String projectId, List<String> yamlFilePath) {
    gitSyncErrorRepository.removeByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePathIn(
        accountId, orgId, projectId, yamlFilePath);
  }

  @Override
  public void upsertGitSyncErrors(GitFileChange failedChange, String errorMessage, boolean fullSyncPath,
      YamlGitConfigDTO yamlGitConfig, boolean gitToHarness) {
    if (gitToHarness) {
      upsertGitToHarnessError(failedChange, errorMessage, yamlGitConfig);
    } else {
      upsertHarnessToGitError(failedChange, errorMessage, fullSyncPath, yamlGitConfig);
    }
  }

  private void upsertHarnessToGitError(
      GitFileChange failedChange, String errorMessage, boolean fullSyncPath, YamlGitConfigDTO yamlGitConfig) {
    log.info(String.format("Upsert haress to git issue for file: %s", failedChange.getFilePath()));

    gitSyncErrorRepository.upsertGitError(failedChange.getAccountId(), failedChange.getFilePath(), HARNESS_TO_GIT,
        errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info", fullSyncPath,
        failedChange.getChangeType(), getHarnessToGitErrorDetails(failedChange, fullSyncPath),
        yamlGitConfig.getGitConnectorId(), yamlGitConfig.getRepo(), yamlGitConfig.getBranch(),
        GitFileLocationHelper.getRootPathSafely(failedChange.getFilePath()), yamlGitConfig.getIdentifier(),
        yamlGitConfig.getProjectId(), yamlGitConfig.getOrganizationId());
  }

  private void upsertGitToHarnessError(
      GitFileChange failedGitFileChange, String errorMessage, YamlGitConfigDTO yamlGitConfig) {
    log.info("Upsert git to harness sync issue for file: [{}]", failedGitFileChange.getFilePath());

    GitToHarnessErrorDetails gitToHarnessErrorDetails = getGitToHarnessErrorDetails(failedGitFileChange);
    final GitSyncError previousGitSyncError = gitSyncErrorRepository.findByAccountIdAndYamlFilePathAndGitSyncDirection(
        failedGitFileChange.getAccountId(), failedGitFileChange.getFilePath(), GIT_TO_HARNESS);
    addPreviousCommitDetailsToErrorDetails(failedGitFileChange, gitToHarnessErrorDetails, previousGitSyncError);
    gitSyncErrorRepository.upsertGitError(failedGitFileChange.getAccountId(), failedGitFileChange.getFilePath(),
        GIT_TO_HARNESS, errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info",
        false, failedGitFileChange.getChangeType(), gitToHarnessErrorDetails, yamlGitConfig.getGitConnectorId(),
        yamlGitConfig.getRepo(), yamlGitConfig.getBranch(),
        GitFileLocationHelper.getRootPathSafely(failedGitFileChange.getFilePath()), yamlGitConfig.getIdentifier(),
        yamlGitConfig.getProjectId(), yamlGitConfig.getOrganizationId());
  }

  private void addPreviousCommitDetailsToErrorDetails(GitFileChange failedGitFileChange,
      GitToHarnessErrorDetails gitToHarnessErrorDetails, GitSyncError previousGitSyncError) {
    if (previousGitSyncError != null) {
      if (!failedGitFileChange.isChangeFromAnotherCommit()) {
        GitToHarnessErrorDetails oldGitToHarnessErrorDetails =
            (GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails();
        // Reading the previous errors of the file
        List<GitSyncError> previousGitSyncErrors =
            new ArrayList<>(emptyIfNull(oldGitToHarnessErrorDetails.getPreviousErrors()));
        List<String> previousCommitIds =
            new ArrayList<>(emptyIfNull(oldGitToHarnessErrorDetails.getPreviousCommitIdsWithError()));
        log.info("Adding the error with the commitId [{}] to the previous commit list of file [{}]",
            getCommitIdOfError(previousGitSyncError), failedGitFileChange.getFilePath());
        // Setting the value of the previous details as empty as this record will not go to the previous list
        previousGitSyncError.setUuid(generateUuid());
        ((GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails())
            .setPreviousErrors(Collections.emptyList());
        ((GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails())
            .setPreviousCommitIdsWithError(Collections.emptyList());
        // adding the new entry to the list
        previousGitSyncErrors.add(previousGitSyncError);
        previousCommitIds.add(getCommitIdOfError(previousGitSyncError));
        // For the new error updating all its fields with the appropriate values
        gitToHarnessErrorDetails.setPreviousErrors(previousGitSyncErrors);
        gitToHarnessErrorDetails.setPreviousCommitIdsWithError(previousCommitIds);
      }
    } else {
      log.info("Creating a new error record for the file [{}] in account", failedGitFileChange.getFilePath());
      gitToHarnessErrorDetails.setPreviousErrors(Collections.emptyList());
      gitToHarnessErrorDetails.setPreviousCommitIdsWithError(Collections.emptyList());
    }
  }

  private HarnessToGitErrorDetails getHarnessToGitErrorDetails(GitFileChange failedChange, boolean fullSyncPath) {
    return HarnessToGitErrorDetails.builder().fullSyncPath(fullSyncPath).build();
  }

  private GitToHarnessErrorDetails getGitToHarnessErrorDetails(GitFileChange failedGitFileChange) {
    String failedCommitId = failedGitFileChange.getCommitId() != null ? failedGitFileChange.getCommitId() : "";
    if (failedCommitId.equals("")) {
      log.info("Unexpected behaviour: The git commitId is null for the git to harness error");
    }
    return GitToHarnessErrorDetails.builder()
        .gitCommitId(failedCommitId)
        .yamlContent(failedGitFileChange.getFileContent())
        .commitTime(failedGitFileChange.getCommitTimeMs())
        .commitMessage(failedGitFileChange.getCommitMessage())
        .build();
  }

  @Override
  public List<GitSyncError> getActiveGitToHarnessSyncErrors(String accountId, String gitConnectorId, String repoName,
      String branchName, String rootFolder, long fromTimestamp) {
    return gitSyncErrorRepository.getActiveGitSyncError(
        accountId, fromTimestamp, GIT_TO_HARNESS, gitConnectorId, repoName, branchName, rootFolder);
  }

  @Override
  public boolean deleteGitSyncErrors(List<String> errorIds, String accountId) {
    return gitSyncErrorRepository.deleteByIds(errorIds).wasAcknowledged();
  }
}
