package io.harness.gitsync.core.impl;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.HarnessSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;
import io.harness.gitsync.core.dao.api.repositories.YamlSuccessfulChange.YamlSuccessfulChangeRepository;
import io.harness.gitsync.core.service.YamlSuccessfulChangeService;
import io.harness.logging.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;

import java.util.Optional;

@Singleton
@Slf4j
public class YamlSuccessfulChangeServiceImpl implements YamlSuccessfulChangeService {
  @Inject YamlSuccessfulChangeRepository yamlSuccessfulChangeRepository;

  @Override
  public String upsert(YamlSuccessfulChange yamlSuccessfulChange) {
    Optional<YamlSuccessfulChange> yamlSuccessfulChangeExisting =
        yamlSuccessfulChangeRepository.findByAccountIdAndYamlFilePath(
            yamlSuccessfulChange.getAccountId(), yamlSuccessfulChange.getYamlFilePath());
    // as the race condition would be minimum, so using delete and save for upsert
    yamlSuccessfulChangeExisting.ifPresent(successfulChange -> yamlSuccessfulChangeRepository.delete(successfulChange));
    return yamlSuccessfulChangeRepository.save(yamlSuccessfulChange).getUuid();
  }

  @Override
  public void updateOnHarnessChangeSet(YamlChangeSet savedYamlChangeset) {
    if (harnessToGitChange(savedYamlChangeset)) {
      try (AccountLogContext ignore = new AccountLogContext(savedYamlChangeset.getAccountId(), OVERRIDE_ERROR)) {
        logger.info("updating SuccessfulChange for  harness -> git changeset [{}]", savedYamlChangeset.getUuid());
        for (GitFileChange gitFileChange : ListUtils.emptyIfNull(savedYamlChangeset.getGitFileChanges())) {
          final YamlSuccessfulChange yamlSuccessfulChange =
              YamlSuccessfulChange.builder()
                  .accountId(savedYamlChangeset.getAccountId())
                  .yamlFilePath(gitFileChange.getFilePath())
                  .changeRequestTS(System.currentTimeMillis())
                  .changeProcessedTS(System.currentTimeMillis())
                  .changeSource(YamlSuccessfulChange.ChangeSource.HARNESS)
                  .changeDetail(
                      HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(savedYamlChangeset.getUuid()).build())
                  .build();
          final String updatedId = upsert(yamlSuccessfulChange);
          logger.info(
              "updated SuccessfulChange for file = [{}], changeRequestTS =[{}], changeProcessedTS=[{}], uuid= [{}] ",
              yamlSuccessfulChange.getYamlFilePath(), yamlSuccessfulChange.getChangeRequestTS(),
              yamlSuccessfulChange.getChangeProcessedTS(), updatedId);
        }
        logger.info("updated SuccessfulChange for  harness -> git changeset [{}]", savedYamlChangeset.getUuid());
      }
    }
  }

  private boolean harnessToGitChange(YamlChangeSet savedYamlChangeset) {
    return !savedYamlChangeset.isGitToHarness();
  }
}
