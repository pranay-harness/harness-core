package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Singleton;

import lombok.Setter;
import software.wings.beans.Account;

@Singleton
public class AccountGenerator {
  @Setter Account account;

  public enum Accounts {
    GENERIC_TEST,
  }

  public Account ensurePredefined(long seed, Accounts predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Account ensureGenericTest(long seed) {
    if (account == null) {
      account = anAccount().withUuid(GLOBAL_ACCOUNT_ID).build();
    }
    return account;
  }

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}