package io.harness.repositories.executions;

import static io.harness.rule.OwnerRule.JAMIE;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.lock.PersistentLocker;
import io.harness.lock.mongo.AcquiredDistributedLock;
import io.harness.pms.plan.execution.AccountExecutionMetadata;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.PIPELINE)
public class AccountExecutionMetadataRepositoryCustomImplTest extends PipelineServiceTestBase {
  private static final String ACCOUNT_ID = "accountId";
  private static final String CI = "ci";
  private static final String PRIVATE_REPO_BUILD_CI = "ci_private_build";
  @Inject PersistentLocker persistentLocker;
  @Inject AccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @RealMongo
  public void testUpdateAccountExecutionMetadata() {
    Mockito.when(persistentLocker.tryToAcquireLock(any(), any())).thenReturn(AcquiredDistributedLock.builder().build());
    accountExecutionMetadataRepository.updateAccountExecutionMetadata(
        ACCOUNT_ID, Sets.newHashSet("cd"), System.currentTimeMillis());
    Optional<AccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(ACCOUNT_ID);
    assertThat(accountExecutionMetadata.isPresent()).isTrue();
    assertThat(accountExecutionMetadata.get().getModuleToExecutionCount().get("cd")).isEqualTo(1L);
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  @RealMongo
  public void testUpdateAccountExecutionMetadataCIPrivateRepo() {
    Mockito.when(persistentLocker.tryToAcquireLock(any(), any())).thenReturn(AcquiredDistributedLock.builder().build());
    accountExecutionMetadataRepository.updateAccountExecutionMetadata(
        ACCOUNT_ID, Sets.newHashSet(PRIVATE_REPO_BUILD_CI, CI), 1636606650379L);
    Optional<AccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(ACCOUNT_ID);
    assertThat(accountExecutionMetadata.isPresent()).isTrue();
    assertThat(accountExecutionMetadata.get().getModuleToExecutionCount().get(CI)).isEqualTo(1L);
    assertThat(accountExecutionMetadata.get().getModuleToExecutionCount().get(PRIVATE_REPO_BUILD_CI)).isEqualTo(1L);
    assertThat(accountExecutionMetadata.get().getModuleToExecutionInfoMap().get(PRIVATE_REPO_BUILD_CI)).isEqualTo(null);
  }
}