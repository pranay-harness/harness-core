package software.wings.service;

import static com.google.common.truth.Truth.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class AccountServiceTest extends WingsBaseTest {
  @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldSaveAccount() throws Exception {
    Account account = accountService.save(anAccount().withCompanyName("Wings").build());
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldDeleteAccount() throws Exception {
    String accountId = wingsPersistence.save(anAccount().withCompanyName("Wings").build());
    accountService.delete(accountId);
    assertThat(wingsPersistence.get(Account.class, accountId)).isNull();
  }

  @Test
  public void shouldUpdateCompanyName() throws Exception {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName("Wings").build());
    account.setCompanyName("Wings Software");
    accountService.update(account);
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  public void shouldGetAccountByName() throws Exception {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName("Wings").build());
    assertThat(accountService.getByName("Wings")).isEqualTo(account);
  }

  @Test
  public void shouldGetAccount() throws Exception {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName("Wings").build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
  }
}
