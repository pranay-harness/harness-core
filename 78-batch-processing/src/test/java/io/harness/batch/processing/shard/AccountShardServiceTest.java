package io.harness.batch.processing.shard;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.PodInfoConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class AccountShardServiceTest extends CategoryTest {
  @InjectMocks private AccountShardService accountShardService;
  @Mock private BatchMainConfig mainConfig;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;

  private final String POD_NAME = "batch-processing-0";
  private final int REPLICAS = 2;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetActiveAccounts() {
    PodInfoConfig podInfoConfig = PodInfoConfig.builder().name(POD_NAME).replica(REPLICAS).build();
    when(mainConfig.getPodInfoConfig()).thenReturn(podInfoConfig);
    ImmutableList<Account> accounts = ImmutableList.of(
        getAccount("accountId1"), getAccount("accountId2"), getAccount("accountId3"), getAccount("accountId4"));
    when(cloudToHarnessMappingService.getCeEnabledAccounts()).thenReturn(accounts);
    List<Account> ceEnabledAccounts = accountShardService.getCeEnabledAccounts();
    assertThat(ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("accountId2", "accountId4");
  }

  private Account getAccount(String uuid) {
    return Account.Builder.anAccount().withUuid(uuid).build();
  }
}
