package io.harness.scheduler;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob.SERVICE_GUARD_DATA_COLLECTION_CRON;
import static io.harness.jobs.sg247.timeseries.ServiceGuardTimeSeriesAnalysisJob.SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.VerificationBaseTest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.rest.RestResponse;
import io.harness.security.EncryptionUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.dl.WingsPersistence;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.LicenseUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Pranjal on 10/09/2018
 */

@SetupScheduler
public class ServiceGuardPollerTest extends VerificationBaseTest {
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject @Named("verificationServiceExecutor") private ScheduledExecutorService executorService;
  @Inject private ServiceGuardAccountPoller serviceGuardAccountPoller;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private VerificationManagerClient verificationManagerClient;

  @Test
  @Category(UnitTests.class)
  public void test_triggerServiceGuardCron() throws IOException, IllegalAccessException {
    List<Account> accounts = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Account account = createAccount(AccountType.PAID, AccountStatus.ACTIVE);
      account.setUuid("account-" + i);
      accounts.add(account);
    }

    Call<RestResponse<PageResponse<Account>>> accountCall = mock(Call.class);
    when(accountCall.execute())
        .thenReturn(
            Response.success(new RestResponse<>(aPageResponse().withResponse(new NonRemovableList(accounts)).build())));

    when(verificationManagerClient.getAccounts(anyString())).thenReturn(accountCall);
    writeField(serviceGuardAccountPoller, "verificationManagerClient", verificationManagerClient, true);

    ServiceGuardAccountPoller.POLL_INTIAL_DELAY_SEOONDS = 1;
    ServiceGuardAccountPoller.POLL_REPEAT_INTERNAL_SEOONDS = 5;
    serviceGuardAccountPoller.scheduleAccountPolling();
    sleep(ofMillis(10000));
    accounts.forEach(account -> {
      assertThat(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON)).isTrue();
      assertThat(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)).isTrue();
      assertThat(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)).isTrue();
    });

    when(accountCall.execute())
        .thenReturn(Response.success(new RestResponse<>(aPageResponse().withResponse(Lists.newArrayList()).build())));
    sleep(ofMillis(10000));

    accounts.forEach(account -> {
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON));
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));
      assertFalse(jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON));
    });
  }

  private Account createAccount(String type, String status) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(type);
    licenseInfo.setAccountStatus(status);
    byte[] licenseInfoEncrypted =
        EncryptionUtils.encrypt(LicenseUtils.convertToString(licenseInfo).getBytes(Charset.forName("UTF-8")), null);

    Account account = anAccount()
                          .withUuid(generateUuid())
                          .withCompanyName(generateUuid())
                          .withAccountName(generateUuid())
                          .withAccountKey("ACCOUNT_KEY")
                          .withLicenseInfo(licenseInfo)
                          .build();
    account.setEncryptedLicenseInfo(licenseInfoEncrypted);
    return account;
  }

  private static class NonRemovableList<E> extends ArrayList<E> {
    NonRemovableList(List<E> elements) {
      super(elements);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return true;
    }
  }
}
