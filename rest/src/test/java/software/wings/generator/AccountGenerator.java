package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Account.ACCOUNT_NAME_KEY;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Setter;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.generator.OwnerManager.Owners;
import software.wings.service.intfc.AccountService;

@Singleton
public class AccountGenerator {
  @Inject AccountService accountService;

  @Inject WingsPersistence wingsPersistence;

  @Setter Account account;

  public enum Accounts {
    GENERIC_TEST,
  }

  public Account ensurePredefined(Randomizer.Seed seed, Owners owners, Accounts predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Account exists(Account account) {
    return wingsPersistence.createQuery(Account.class).filter(ACCOUNT_NAME_KEY, account.getAccountName()).get();
  }

  private Account ensureGenericTest(Randomizer.Seed seed, Owners owners) {
    if (this.account != null) {
      return this.account;
    }

    final Account accountObj = anAccount().withAccountName("Harness").withCompanyName("Harness").build();
    this.account = exists(accountObj);

    if (this.account == null) {
      this.account = accountService.save(accountObj);
    }
    return this.account;
  }

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}
