package io.harness.gitsync.core.impl;

import static io.harness.gitsync.core.beans.YamlSuccessfulChange.ChangeSource.HARNESS;

import static java.lang.String.format;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.HarnessSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.SuccessfulChangeDetail;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;
import io.harness.gitsync.core.dtos.YamlFilterResult;
import io.harness.gitsync.core.dtos.YamlFilterResult.YamlFilterResultBuilder;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlSuccessfulChangeService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ChangeSetRequestTimeFilter {
  private final YamlSuccessfulChangeService yamlSuccessfulChangeService;
  private final YamlChangeSetService yamlChangeSetService;

  public YamlFilterResult filterFiles(
      List<GitFileChange> gitFileChangeList, String accountId, YamlGitConfigDTO yamlGitConfig) {
    log.info("Started applying ChangeRequestTimeFilter for files");
    final YamlFilterResultBuilder filterResultBuilder = YamlFilterResult.builder();
    for (GitFileChange gitFileChange : gitFileChangeList) {
      if (commitTimeMissing(gitFileChange)) {
        filterResultBuilder.filteredFile(gitFileChange);
      } else {
        filterFileWithCommitTime(accountId, gitFileChange, filterResultBuilder, yamlGitConfig);
      }
    }
    log.info("Successfully applied ChangeRequestTimeFilter for files");
    return filterResultBuilder.build();
  }

  private void filterFileWithCommitTime(String accountId, GitFileChange gitFileChange,
      YamlFilterResultBuilder filterResultBuilder, YamlGitConfigDTO yamlGitConfig) {
    final Optional<YamlSuccessfulChange> yamlSuccessfulChange = yamlSuccessfulChangeService.get(
        accountId, yamlGitConfig.getOrganizationId(), yamlGitConfig.getProjectId(), gitFileChange.getFilePath());

    if (!yamlSuccessfulChange.isPresent()) {
      filterResultBuilder.filteredFile(gitFileChange);
    } else {
      handleWhenSuccessfulChangeFound(gitFileChange, yamlSuccessfulChange.get(), filterResultBuilder);
    }
  }

  private void handleWhenSuccessfulChangeFound(GitFileChange gitFileChange, YamlSuccessfulChange yamlSuccessfulChange,
      YamlFilterResultBuilder filterResultBuilder) {
    if (!isChangeRequestInOrder(gitFileChange, yamlSuccessfulChange)) {
      handleChangeOutOfOrder(gitFileChange, yamlSuccessfulChange, filterResultBuilder);
    } else if (lastSuccessFulChangeWasHarnessAndYetToProcess(yamlSuccessfulChange)) {
      handleHarnessChangesetNotProcessed(gitFileChange, yamlSuccessfulChange, filterResultBuilder);
    } else {
      filterResultBuilder.filteredFile(gitFileChange);
    }
  }

  private boolean lastSuccessFulChangeWasHarnessAndYetToProcess(YamlSuccessfulChange yamlSuccessfulChange) {
    return lastSuccessfulChangeWasHarness(yamlSuccessfulChange) && !hasHarnessChangesetProcessed(yamlSuccessfulChange);
  }

  private void handleHarnessChangesetNotProcessed(GitFileChange gitFileChange,
      YamlSuccessfulChange yamlSuccessfulChange, YamlFilterResultBuilder filterResultBuilder) {
    final String skipMessage = format("Conflict detected: Override due to change from Harness UI/API at [%s]",
        dateStringInGMT(yamlSuccessfulChange.getChangeRequestTS()));

    final HarnessSuccessFulChangeDetail changeDetail =
        (HarnessSuccessFulChangeDetail) yamlSuccessfulChange.getChangeDetail();
    log.info(" Skipping file [{}] with reason =[{}]. Harness -> git changeset [{}] still in queue.",
        gitFileChange.getFilePath(), skipMessage, changeDetail.getYamlChangeSetId());

    filterResultBuilder.excludedFilePathWithReason(gitFileChange.getFilePath(), skipMessage);
  }

  private boolean hasHarnessChangesetProcessed(YamlSuccessfulChange yamlSuccessfulChange) {
    final Optional<YamlChangeSet> yamlChangeSet = getYamlChangeSet(yamlSuccessfulChange);
    if (yamlChangeSet.isPresent()) {
      log.info("Yamlchangeset id =[{}], status =[{}]", yamlChangeSet.get().getUuid(), yamlChangeSet.get().getStatus());
      return YamlChangeSet.terminalStatusList.contains(yamlChangeSet.get().getStatus());
    }
    return true;
  }

  private Optional<YamlChangeSet> getYamlChangeSet(YamlSuccessfulChange yamlSuccessfulChange) {
    final SuccessfulChangeDetail changeDetail = yamlSuccessfulChange.getChangeDetail();
    if (changeDetail != null) {
      final HarnessSuccessFulChangeDetail harnessChangeDetail = (HarnessSuccessFulChangeDetail) changeDetail;
      final String yamlChangeSetId = harnessChangeDetail.getYamlChangeSetId();
      if (yamlChangeSetId != null) {
        return yamlChangeSetService.get(yamlSuccessfulChange.getAccountId(), yamlChangeSetId);
      }
    }
    return Optional.empty();
  }

  private boolean lastSuccessfulChangeWasHarness(YamlSuccessfulChange yamlSuccessfulChange) {
    return HARNESS == (yamlSuccessfulChange.getChangeSource());
  }

  private void handleChangeOutOfOrder(GitFileChange gitFileChange, YamlSuccessfulChange yamlSuccessfulChange,
      YamlFilterResultBuilder filterResultBuilder) {
    final String skipMessage = format(
        "Conflict detected: File change request time [%s] is less than last successfully processed change request time [%s]",
        dateStringInGMT(gitFileChange.getCommitTimeMs()), dateStringInGMT(yamlSuccessfulChange.getChangeRequestTS()));
    log.info(" Skipping file [{}] with reason =[{}]. last Successful change detail = [{}] ",
        gitFileChange.getFilePath(), skipMessage, yamlSuccessfulChange.getChangeDetail());
    filterResultBuilder.excludedFilePathWithReason(gitFileChange.getFilePath(), skipMessage);
  }

  private boolean isChangeRequestInOrder(GitFileChange gitFileChange, YamlSuccessfulChange yamlSuccessfulChange) {
    return yamlSuccessfulChange.getChangeRequestTS() < gitFileChange.getCommitTimeMs();
  }

  private boolean commitTimeMissing(GitFileChange gitFileChange) {
    return gitFileChange.getCommitTimeMs() == null;
  }

  private String dateStringInGMT(long timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    sdf.setTimeZone(TimeZone.getTimeZone(TimeZone.getTimeZone("Etc/UTC").toZoneId()));
    final Date date = new Date();
    date.setTime(timestamp);
    return sdf.format(date);
  }
}
