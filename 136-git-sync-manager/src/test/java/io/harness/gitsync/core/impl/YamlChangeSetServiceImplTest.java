package io.harness.gitsync.core.impl;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.repositories.yamlChangeSet.YamlChangeSetRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlChangeSetServiceImplTest extends GitSyncTestBase {
  public static final String ACCOUNT_ID = "accountId";
  @Inject private YamlChangeSetRepository yamlChangeSetRepository;
  @Inject YamlChangeSetServiceImpl yamlChangeSetService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateStatusAndIncrementRetryCountForYamlChangeSets() {
    YamlChangeSet yamlChangeSet = yamlChangeSetRepository.save(
        YamlChangeSet.builder().accountIdentifier(ACCOUNT_ID).status(Status.SKIPPED.name()).build());
    assertThat(yamlChangeSetService.updateStatusAndIncrementRetryCountForYamlChangeSets(ACCOUNT_ID, Status.QUEUED,
                   Collections.singletonList(YamlChangeSet.Status.valueOf(yamlChangeSet.getStatus())),
                   Collections.singletonList(yamlChangeSet.getUuid())))
        .isTrue();
    Optional<YamlChangeSet> yamlChangeSet1 = yamlChangeSetService.get(ACCOUNT_ID, yamlChangeSet.getUuid());
    assertThat(yamlChangeSet1.isPresent()).isTrue();
    assertThat(yamlChangeSet1.get().getRetryCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateStatusForGivenYamlChangeSets() {
    YamlChangeSet yamlChangeSet = yamlChangeSetRepository.save(
        YamlChangeSet.builder().accountIdentifier(ACCOUNT_ID).status(Status.SKIPPED.name()).build());
    assertThat(yamlChangeSetService.updateStatusForGivenYamlChangeSets(ACCOUNT_ID, Status.QUEUED,
                   Collections.singletonList(YamlChangeSet.Status.valueOf(yamlChangeSet.getStatus())),
                   Collections.singletonList(yamlChangeSet.getUuid())))
        .isTrue();
  }
}
