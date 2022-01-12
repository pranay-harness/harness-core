/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.executions;

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
public class CDCDAccountExecutionMetadataRepositoryCustomImplTest extends PipelineServiceTestBase {
  private static final String ACCOUNT_ID = "accountId";
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
    accountExecutionMetadataRepository.updateAccountExecutionMetadata(
        ACCOUNT_ID, Sets.newHashSet("ci"), System.currentTimeMillis());
    Optional<AccountExecutionMetadata> accountExecutionMetadata =
        accountExecutionMetadataRepository.findByAccountId(ACCOUNT_ID);
    assertThat(accountExecutionMetadata.isPresent()).isTrue();
    assertThat(accountExecutionMetadata.get().getModuleToExecutionCount().get("cd")).isEqualTo(1L);
  }
}
