package io.harness.integration;

import static io.harness.jobs.VerificationJob.VERIFICATION_CRON_GROUP;
import static io.harness.jobs.VerificationJob.VERIFICATION_CRON_NAME;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.VerificationBaseIntegrationTest;
import io.harness.jobs.MetricDataProcessorJob;
import io.harness.jobs.VerificationJob;
import io.harness.scheduler.PersistentScheduler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureFlag;
import software.wings.beans.LicenseInfo;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.LicenseUtil;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Created by Pranjal on 10/05/2018
 */
@Ignore
public class VerificationJobIntegrationTest extends VerificationBaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(VerificationJobIntegrationTest.class);

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private VerificationJob job;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testExecute() {
    job.addJob(jobScheduler);

    // Check if short cron is completed or not.
    assertThat(jobScheduler.checkExists(accountId, MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP)).isFalse();

    // Check long running cron is still present.
    assertThat(jobScheduler.deleteJob(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)).isTrue();
  }

  @Test
  public void testMultiAccountCron() throws SchedulerException, TimeoutException, InterruptedException {
    Account account1 = createAccount(AccountType.PAID, AccountStatus.ACTIVE);
    Account account2 = createAccount(AccountType.TRIAL, AccountStatus.EXPIRED);
    Account account3 = createAccount(AccountType.FREEMIUM, AccountStatus.ACTIVE);

    job.addJob(jobScheduler);

    // Check if short cron is completed or accountId1.
    assertThat(jobScheduler.checkExists(account1.getUuid(), MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP))
        .isFalse();

    // Check if short cron is completed or accountId1.
    assertThat(jobScheduler.checkExists(account2.getUuid(), MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP))
        .isFalse();

    // Check if short cron is completed or accountId1.
    assertThat(jobScheduler.checkExists(account3.getUuid(), MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP))
        .isFalse();

    // Check long running cron is still present.
    assertThat(jobScheduler.deleteJob(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)).isTrue();
  }

  private Account createAccount(String type, String status) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(type);
    licenseInfo.setAccountStatus(status);
    byte[] licenseInfoEncrypted =
        EncryptionUtils.encrypt(LicenseUtil.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);

    Account account = anAccount()
                          .withCompanyName(UUID.randomUUID().toString())
                          .withAccountName(UUID.randomUUID().toString())
                          .withAccountKey("ACCOUNT_KEY")
                          .withLicenseInfo(licenseInfo)
                          .build();
    account.setEncryptedLicenseInfo(licenseInfoEncrypted);
    String accountId = wingsPersistence.save(account);

    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", "TRIAL_SUPPORT").get();

    Set<String> accountIds = new HashSet<>();
    accountIds.add(accountId);
    featureFlag.setAccountIds(accountIds);
    wingsPersistence.save(featureFlag);
    return account;
  }
}
