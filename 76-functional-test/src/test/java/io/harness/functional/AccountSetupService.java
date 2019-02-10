package io.harness.functional;

import static io.harness.generator.AccountGenerator.Accounts.GENERIC_TEST;
import static java.time.Duration.ofMinutes;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.filesystem.FileIo;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.resource.Project;
import software.wings.beans.Account;

import java.io.File;

@Singleton
public class AccountSetupService {
  @Inject OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;

  public Account ensureAccount() {
    String directoryPath = Project.rootDirectory(AbstractFunctionalTest.class);
    final File lockfile = new File(directoryPath, GENERIC_TEST.name());
    try {
      if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
        Account accountObj = anAccount().withAccountName("Harness").withCompanyName("Harness").build();

        final Account account = accountGenerator.exists(accountObj);
        if (account != null) {
          return account;
        }
        final Seed seed = new Seed(0);
        Owners owners = ownerManager.create();
        return accountGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
      }
    } finally {
      FileIo.releaseLock(lockfile);
    }
    throw new RuntimeException("Unknown error occurred during account setup");
  }
}
