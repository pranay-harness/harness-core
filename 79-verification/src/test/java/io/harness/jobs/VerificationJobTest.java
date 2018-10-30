package io.harness.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.VerificationBaseTest;
import io.harness.scheduler.PersistentScheduler;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.LicenseUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Pranjal on 10/09/2018
 */
@Ignore
public class VerificationJobTest extends VerificationBaseTest {
  @Mock @Named("JobScheduler") private PersistentScheduler jobScheduler;
  @Inject private VerificationJob job;
  JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);

  @Test
  public void test_triggerDataProcessorCron() {
    Account account = createAccount(AccountType.PAID, AccountStatus.ACTIVE);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    job.setQuartzScheduler(jobScheduler);
    job.triggerDataProcessorCron(accounts);

    // Will be called twice 1 for each APM and log
    verify(jobScheduler, times(2)).scheduleJob(any(JobDetail.class), any(Trigger.class));
  }

  @Test
  public void test_deleteCrons() {
    Account account = createAccount(AccountType.PAID, AccountStatus.ACTIVE);
    List<Account> accounts = new ArrayList<>();
    accounts.add(account);
    job.setQuartzScheduler(jobScheduler);
    job.deleteCrons(accounts);

    // Will be called twice 1 for each APM and log
    verify(jobScheduler, times(2)).checkExists(anyString(), anyString());
  }

  private Account createAccount(String type, String status) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(type);
    licenseInfo.setAccountStatus(status);
    byte[] licenseInfoEncrypted =
        EncryptionUtils.encrypt(LicenseUtil.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);

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
