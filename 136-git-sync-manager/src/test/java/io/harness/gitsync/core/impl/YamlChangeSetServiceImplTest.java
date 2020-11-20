package io.harness.gitsync.core.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.core.dao.api.repositories.YamlChangeSet.YamlChangeSetRepository;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.Optional;

public class YamlChangeSetServiceImplTest extends GitSyncBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  @Inject private YamlChangeSetRepository yamlChangeSetRepository;
  @Inject YamlChangeSetServiceImpl yamlChangeSetService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateStatusAndIncrementRetryCountForYamlChangeSets() {
    YamlChangeSet yamlChangeSet =
        yamlChangeSetRepository.save(YamlChangeSet.builder().accountId(ACCOUNT_ID).status(Status.SKIPPED).build());
    assertThat(
        yamlChangeSetService.updateStatusAndIncrementRetryCountForYamlChangeSets(ACCOUNT_ID, Status.QUEUED,
            Collections.singletonList(yamlChangeSet.getStatus()), Collections.singletonList(yamlChangeSet.getUuid())))
        .isTrue();
    Optional<YamlChangeSet> yamlChangeSet1 = yamlChangeSetService.get(ACCOUNT_ID, yamlChangeSet.getUuid());
    assertThat(yamlChangeSet1.isPresent()).isTrue();
    assertThat(yamlChangeSet1.get().getRetryCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateStatusForGivenYamlChangeSets() {
    YamlChangeSet yamlChangeSet =
        yamlChangeSetRepository.save(YamlChangeSet.builder().accountId(ACCOUNT_ID).status(Status.SKIPPED).build());
    assertThat(
        yamlChangeSetService.updateStatusForGivenYamlChangeSets(ACCOUNT_ID, Status.QUEUED,
            Collections.singletonList(yamlChangeSet.getStatus()), Collections.singletonList(yamlChangeSet.getUuid())))
        .isTrue();
  }
}
