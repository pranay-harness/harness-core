package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.DelegateConfiguration;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseManager;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateGalleryService;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class AccountServiceTest extends WingsBaseTest {
  @Mock private LicenseManager licenseManager;

  @Mock private AppService appService;
  @Mock private SettingsService settingsService;
  @Mock private JobScheduler jobScheduler;
  @Mock private TemplateGalleryService templateGalleryService;

  @InjectMocks @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldSaveAccount() {
    Account account = accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
    verify(settingsService).createDefaultAccountSettings(account.getUuid());
    verify(jobScheduler).deleteJob(eq(account.getUuid()), anyString());
  }

  @Test
  public void shouldDeleteAccount() {
    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());
    accountService.delete(accountId);
    assertThat(wingsPersistence.get(Account.class, accountId)).isNull();
    verify(appService).deleteByAccountId(accountId);
    verify(settingsService).deleteByAccountId(accountId);
    verify(templateGalleryService).deleteByAccountId(accountId);
  }

  @Test
  public void shouldUpdateCompanyName() {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withCompanyName("Wings").withAccountName("Wings").build());
    account.setCompanyName(HARNESS_NAME);
    accountService.update(account);
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByCompanyName() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByAccountName() {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withAccountName(HARNESS_NAME).withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByAccountName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  public void shouldGetAccount() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetDelegateConfiguration() {
    String accountId =
        wingsPersistence.save(anAccount()
                                  .withCompanyName(HARNESS_NAME)
                                  .withDelegateConfiguration(DelegateConfiguration.builder()
                                                                 .watcherVersion("1.0.1")
                                                                 .delegateVersions(asList("1.0.0", "1.0.1"))
                                                                 .build())
                                  .build());
    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "1.0.1")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("1.0.0", "1.0.1"));
  }

  @Test
  public void shouldGetDelegateConfigurationFromGlobalAccount() {
    wingsPersistence.save(anAccount()
                              .withUuid(GLOBAL_ACCOUNT_ID)
                              .withCompanyName(HARNESS_NAME)
                              .withDelegateConfiguration(DelegateConfiguration.builder()
                                                             .watcherVersion("globalVersion")
                                                             .delegateVersions(asList("globalVersion"))
                                                             .build())
                              .build());

    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());

    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "globalVersion")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("globalVersion"));
  }

  @Test
  public void shouldListAllAccounts() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
    assertThat(accountService.listAllAccounts()).isNotEmpty();
    assertThat(accountService.listAllAccounts().get(0)).isNotNull();
  }
}
