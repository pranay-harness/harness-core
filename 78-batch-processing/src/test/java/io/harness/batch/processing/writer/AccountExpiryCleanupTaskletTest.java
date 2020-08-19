package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.batch.processing.service.intfc.AccountExpiryService;
import io.harness.batch.processing.tasklet.AccountExpiryCleanupTasklet;
import io.harness.category.element.UnitTests;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AccountExpiryCleanupTaskletTest extends CategoryTest {
  public static final String ACTIVE_ACCOUNT = "activeAccount";
  public static final String EXPIRED_ACCOUNT = "expiredAccount";
  public static final String GRACE_PERIOD = "gracePeriod";
  @Inject @InjectMocks private AccountExpiryCleanupTasklet accountExpiryCleanupTasklet;
  @Mock private AccountExpiryService accountExpiryService;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void write() {
    when(accountExpiryService.dataPipelineCleanup(any())).thenReturn(true);

    List<Account> accountList = new ArrayList<>();
    Instant instant = Instant.now();
    long activeLicense = instant.plus(10, ChronoUnit.DAYS).toEpochMilli();
    long gracePeriodLicense = instant.minus(10, ChronoUnit.DAYS).toEpochMilli();
    long expiredLicense = instant.minus(20, ChronoUnit.DAYS).toEpochMilli();

    Account activeAccount = new Account();
    activeAccount.setAccountName(ACTIVE_ACCOUNT);
    activeAccount.setCeLicenseInfo(CeLicenseInfo.builder().expiryTime(activeLicense).build());

    Account gracePeriodAccount = new Account();
    gracePeriodAccount.setAccountName(GRACE_PERIOD);
    gracePeriodAccount.setCeLicenseInfo(CeLicenseInfo.builder().expiryTime(gracePeriodLicense).build());

    Account expiredAccount = new Account();
    expiredAccount.setAccountName(EXPIRED_ACCOUNT);
    expiredAccount.setCeLicenseInfo(CeLicenseInfo.builder().expiryTime(expiredLicense).build());

    accountList.add(activeAccount);
    accountList.add(gracePeriodAccount);
    accountList.add(expiredAccount);

    when(cloudToHarnessMappingService.getCeAccountsWithLicense()).thenReturn(accountList);

    accountExpiryCleanupTasklet.execute(null, null);

    ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
    verify(accountExpiryService).dataPipelineCleanup(accountArgumentCaptor.capture());
    Account account = accountArgumentCaptor.getValue();
    assertThat(account.getAccountName()).isEqualTo(EXPIRED_ACCOUNT);
  }
}