package io.harness.gitsync.core.impl;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.GitSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.HarnessSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;
import io.harness.gitsync.core.service.YamlSuccessfulChangeService;
import io.harness.logging.AccountLogContext;
import io.harness.repositories.yamlSuccessfulChange.YamlSuccessfulChangeRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;

@Singleton
@Slf4j
public class YamlSuccessfulChangeServiceImpl implements YamlSuccessfulChangeService {
  @Inject YamlSuccessfulChangeRepository yamlSuccessfulChangeRepository;

  @Override
  public String upsert(YamlSuccessfulChange yamlSuccessfulChange) {
    Optional<YamlSuccessfulChange> yamlSuccessfulChangeExisting =
        yamlSuccessfulChangeRepository.findByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePath(
            yamlSuccessfulChange.getAccountId(), yamlSuccessfulChange.getOrganizationId(),
            yamlSuccessfulChange.getProjectId(), yamlSuccessfulChange.getYamlFilePath());
    // as the race condition would be minimum, so using delete and save for upsert
    yamlSuccessfulChangeExisting.ifPresent(successfulChange -> yamlSuccessfulChangeRepository.delete(successfulChange));
    return yamlSuccessfulChangeRepository.save(yamlSuccessfulChange).getUuid();
  }

  @Override
  public void updateOnHarnessChangeSet(YamlChangeSet savedYamlChangeset) {
    if (harnessToGitChange(savedYamlChangeset)) {
      try (AccountLogContext ignore = new AccountLogContext(savedYamlChangeset.getAccountId(), OVERRIDE_ERROR)) {
        log.info("updating SuccessfulChange for  harness -> git changeset [{}]", savedYamlChangeset.getUuid());
        for (GitFileChange gitFileChange : ListUtils.emptyIfNull(savedYamlChangeset.getGitFileChanges())) {
          final YamlSuccessfulChange yamlSuccessfulChange =
              YamlSuccessfulChange.builder()
                  .accountId(savedYamlChangeset.getAccountId())
                  .yamlFilePath(gitFileChange.getFilePath())
                  .changeRequestTS(System.currentTimeMillis())
                  .changeProcessedTS(System.currentTimeMillis())
                  .changeSource(YamlSuccessfulChange.ChangeSource.HARNESS)
                  .projectId(savedYamlChangeset.getProjectId())
                  .organizationId(savedYamlChangeset.getOrganizationId())
                  .changeDetail(
                      HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(savedYamlChangeset.getUuid()).build())
                  .build();
          final String updatedId = upsert(yamlSuccessfulChange);
          log.info(
              "updated SuccessfulChange for file = [{}], changeRequestTS =[{}], changeProcessedTS=[{}], uuid= [{}] ",
              yamlSuccessfulChange.getYamlFilePath(), yamlSuccessfulChange.getChangeRequestTS(),
              yamlSuccessfulChange.getChangeProcessedTS(), updatedId);
        }
        log.info("updated SuccessfulChange for  harness -> git changeset [{}]", savedYamlChangeset.getUuid());
      }
    }
  }

  @Override
  public void updateOnSuccessfulGitChangeProcessing(
      GitFileChange gitFileChange, String accountId, String orgId, String projectId) {
    try (AccountLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      log.info("updating SuccessfulChange from git for  file [{}]", gitFileChange.getFilePath());
      final YamlSuccessfulChange yamlSuccessfulChange =
          YamlSuccessfulChange.builder()
              .accountId(accountId)
              .yamlFilePath(gitFileChange.getFilePath())
              .changeRequestTS(gitFileChange.getCommitTimeMs())
              .changeProcessedTS(System.currentTimeMillis())
              .changeSource(YamlSuccessfulChange.ChangeSource.GIT)
              .organizationId(orgId)
              .projectId(projectId)
              .changeDetail(GitSuccessFulChangeDetail.builder()
                                .commitId(gitFileChange.getCommitId())
                                .processingCommitId(gitFileChange.getProcessingCommitId())
                                .build())
              .build();
      final String updatedId = upsert(yamlSuccessfulChange);
      log.info("updated SuccessfulChange for file = [{}], changeRequestTS =[{}], changeProcessedTS=[{}], uuid= [{}] ",
          yamlSuccessfulChange.getYamlFilePath(), yamlSuccessfulChange.getChangeRequestTS(),
          yamlSuccessfulChange.getChangeProcessedTS(), updatedId);

      log.info("updated SuccessfulChange from git for  file [{}]", gitFileChange.getFilePath());
    }
  }

  @Override
  public Optional<YamlSuccessfulChange> get(String accountId, String orgId, String projectId, String filePath) {
    return yamlSuccessfulChangeRepository.findByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePath(
        accountId, orgId, projectId, filePath);
  }

  private boolean harnessToGitChange(YamlChangeSet savedYamlChangeset) {
    return !savedYamlChangeset.isGitToHarness();
  }
}
