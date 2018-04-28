package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Setter;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

@Singleton
public class AccountGenerator {
  @Inject AccountService accountService;

  @Setter Account account;

  public enum Accounts {
    GENERIC_TEST,
  }

  public Account ensurePredefined(Randomizer.Seed seed, Accounts predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Account ensureGenericTest(Randomizer.Seed seed) {
    if (account == null) {
      account = accountService.save(anAccount().withAccountName("Harness").withCompanyName("Harness").build());
    }
    return account;
  }

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}
