package io.harness.secret;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import software.wings.beans.Account;
import software.wings.service.intfc.security.SecretManager;

@Singleton
public class EncryptedTextHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  private String secretName = "secretName";
  private String secret = "secret";
  @Inject SecretManager secretManager;

  public String CreateEncryptedText() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    return secretManager.saveSecret(accountId, null, secretName, secret, null, null);
  }
}
