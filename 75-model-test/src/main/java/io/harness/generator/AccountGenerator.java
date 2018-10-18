package io.harness.generator;

import static io.harness.common.GeneratorConstants.defaultAccountId;
import static io.harness.common.GeneratorConstants.delegateAccountSecret;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Account.ACCOUNT_NAME_KEY;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.generator.OwnerManager.Owners;
import lombok.Setter;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.dl.WingsPersistence;
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
        return ensureGenericTest();
      default:
        unhandled(predefined);
    }

    return null;
  }

  public Account exists(Account account) {
    return wingsPersistence.createQuery(Account.class).filter(ACCOUNT_NAME_KEY, account.getAccountName()).get();
  }

  public Account ensureGenericTest() {
    if (this.account != null) {
      return this.account;
    }

    Account accountObj = anAccount().withAccountName("Harness").withCompanyName("Harness").build();
    this.account = exists(accountObj);
    if (this.account != null) {
      return account;
    }
    accountObj = Account.Builder.anAccount()
                     .withUuid(defaultAccountId)
                     .withAccountName("Harness")
                     .withCompanyName("Harness")
                     .withLicenseInfo(LicenseInfo.builder()
                                          .accountType(AccountType.PAID)
                                          .accountStatus(AccountStatus.ACTIVE)
                                          .expiryTime(-1)
                                          .build())
                     .build();

    accountService.save(accountObj);

    // Update account key to make it work with delegate
    UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
    accountUpdateOperations.set("accountKey", delegateAccountSecret);
    wingsPersistence.update(wingsPersistence.createQuery(Account.class), accountUpdateOperations);

    this.account = accountService.get(defaultAccountId);
    return this.account;
  }

  public Account ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Accounts predefined = random.nextObject(Accounts.class);
    return ensurePredefined(seed, owners, predefined);
  }

  // TODO: Very dummy version, implement this
  public Account randomAccount() {
    return account;
  }
}
