package software.wings.service;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Account.Builder.anAccount;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseManager;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class AccountServiceTest extends WingsBaseTest {
  @Mock private LicenseManager licenseManager;

  @Mock private AppService appService;
  @Mock private SettingsService settingsService;

  @InjectMocks @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldSaveAccount() throws Exception {
    Account account = accountService.save(
        anAccount().withCompanyName("Harness").withAccountName("Harness").withAccountKey("ACCOUNT_KEY").build());
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
    verify(settingsService).createDefaultAccountSettings(account.getUuid());
  }

  @Test
  public void shouldDeleteAccount() throws Exception {
    String accountId = wingsPersistence.save(anAccount().withCompanyName("Harness").build());
    accountService.delete(accountId);
    assertThat(wingsPersistence.get(Account.class, accountId)).isNull();
    verify(appService).deleteByAccountId(accountId);
    verify(settingsService).deleteByAccountId(accountId);
  }

  @Test
  public void shouldUpdateCompanyName() throws Exception {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withCompanyName("Harness").withAccountName("Wings").build());
    account.setCompanyName("Harness Inc");
    accountService.update(account);
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByName() throws Exception {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName("Harness").build());
    assertThat(accountService.getByName("Harness")).isEqualTo(account);
  }

  @Test
  public void shouldGetAccount() throws Exception {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName("Harness").build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
  }
}
