package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.YamlChangeSetKeys;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.runnable.ChangeSetGroupingKey;
import io.harness.repositories.yamlChangeSet.YamlChangeSetRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public class YamlChangeSetServiceImplTest extends GitSyncTestBase {
  public static final String ACCOUNT_ID = "accountId";
  @Inject private YamlChangeSetRepository yamlChangeSetRepository;
  @Inject YamlChangeSetServiceImpl yamlChangeSetService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateStatusAndIncrementRetryCountForYamlChangeSets() {
    YamlChangeSet yamlChangeSet = yamlChangeSetRepository.save(
        YamlChangeSet.builder().accountId(ACCOUNT_ID).status(YamlChangeSetStatus.SKIPPED.name()).build());
    assertThat(
        yamlChangeSetService.updateStatusAndIncrementRetryCountForYamlChangeSets(ACCOUNT_ID, YamlChangeSetStatus.QUEUED,
            Collections.singletonList(YamlChangeSetStatus.valueOf(yamlChangeSet.getStatus())),
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
        YamlChangeSet.builder().accountId(ACCOUNT_ID).status(YamlChangeSetStatus.SKIPPED.name()).build());
    assertThat(yamlChangeSetService.updateStatusForGivenYamlChangeSets(ACCOUNT_ID, YamlChangeSetStatus.QUEUED,
                   Collections.singletonList(YamlChangeSetStatus.valueOf(yamlChangeSet.getStatus())),
                   Collections.singletonList(yamlChangeSet.getUuid())))
        .isTrue();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testChangeSetGroupingKeys() {
    final YamlChangeSet yamlChangeSet =
        yamlChangeSetService.save(YamlChangeSet.builder().accountId("acc").repoUrl("repo").branch("branch").build());
    final Set<ChangeSetGroupingKey> changesetGroupingKeys = yamlChangeSetService.getChangesetGroupingKeys(
        Criteria.where(YamlChangeSetKeys.status).is(YamlChangeSetStatus.QUEUED));
    assertThat(changesetGroupingKeys.size()).isEqualTo(1);
  }
}
