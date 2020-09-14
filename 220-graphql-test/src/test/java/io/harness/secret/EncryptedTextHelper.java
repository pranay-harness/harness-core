package io.harness.secret;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import lombok.Data;
import software.wings.beans.Account;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManager;

@Singleton
public class EncryptedTextHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  private String secretName = "secretName";
  private String secret = "secret";
  @Inject SecretManager secretManager;

  @Data
  public static class CreateEncryptedTextResult {
    String clientMutationId;
    QLEncryptedText secret;
  }

  public String CreateEncryptedText(String name) {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    SecretText secretText = SecretText.builder().name(name).value(secret).build();
    return secretManager.saveSecret(accountId, secretText);
  }
}
