package io.harness.jobs;

import static io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob.SERVICE_GUARD_DATA_COLLECTION_CRON;
import static io.harness.jobs.sg247.timeseries.ServiceGuardTimeSeriesAnalysisJob.SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.jobs.sg247.ServiceGuardMainJob;
import io.harness.scheduler.PersistentScheduler;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.rules.SetupScheduler;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.LicenseUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Pranjal on 10/09/2018
 */

@SetupScheduler
public class ServiceGuardMainJobTest extends VerificationBaseTest {
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private ServiceGuardMainJob job;
  JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);

  @Test
  @Category(UnitTests.class)
  public void test_triggerServiceGuardCron() {
    for (int i = 0; i < 3; i++) {
      Account account = validateServiceGaurdCronsCreate();
      validateServiceGaurdCronsDelete(account);
    }

    List<Account> accounts = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      accounts.add(validateServiceGaurdCronsCreate());
    }

    validateServiceGaurdCronsDelete(accounts.toArray(new Account[0]));
  }

  private Account validateServiceGaurdCronsCreate() {
    Account account = createAccount(AccountType.PAID, AccountStatus.ACTIVE);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    job.setQuartzScheduler(jobScheduler);
    job.triggerServiceGuardCrons(accounts);

    assertTrue(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON));
    assertTrue(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));
    assertTrue(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));

    return account;
  }

  private void validateServiceGaurdCronsDelete(Account... accounts) {
    job.deleteCrons(Arrays.asList(accounts));

    for (Account account : accounts) {
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON));
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));
    }
  }

  private Account createAccount(String type, String status) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(type);
    licenseInfo.setAccountStatus(status);
    byte[] licenseInfoEncrypted =
        EncryptionUtils.encrypt(LicenseUtils.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);

    Account account = anAccount()
                          .withUuid(UUID.randomUUID().toString())
                          .withCompanyName(UUID.randomUUID().toString())
                          .withAccountName(UUID.randomUUID().toString())
                          .withAccountKey("ACCOUNT_KEY")
                          .withLicenseInfo(licenseInfo)
                          .build();
    account.setEncryptedLicenseInfo(licenseInfoEncrypted);
    return account;
  }
}
