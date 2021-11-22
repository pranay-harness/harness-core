package io.harness.ci.repositories;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.rule.OwnerRule.JAMIE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.impl.CIManagerTestBase;
import io.harness.category.element.UnitTests;
import io.harness.ci.pipeline.executions.CIAccountExecutionMetadata;
import io.harness.lock.PersistentLocker;
import io.harness.lock.mongo.AcquiredDistributedLock;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;

import com.google.inject.Inject;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@OwnedBy(CI)
public class CIAccountExecutionMetadataRepositoryCustomImplTest extends CIManagerTestBase {
  private static final String ACCOUNT_ID = "accountId";
  @Inject PersistentLocker persistentLocker;
  @Inject CIAccountExecutionMetadataRepository accountExecutionMetadataRepository;

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  @RealMongo
  public void testUpdateAccountExecutionMetadata() {
    Mockito.when(persistentLocker.tryToAcquireLock(any(), any())).thenReturn(AcquiredDistributedLock.builder().build());
    accountExecutionMetadataRepository.updateAccountExecutionMetadata(ACCOUNT_ID, 1637512542006L);
    Optional<CIAccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(ACCOUNT_ID);
    assertThat(accountExecutionMetadata.isPresent()).isTrue();
    assertThat(accountExecutionMetadata.get().getExecutionCount()).isEqualTo(1L);
    assertThat(accountExecutionMetadata.get().getAccountExecutionInfo().getCountPerMonth().get("2021-11"))
        .isEqualTo(0L);
  }
}
