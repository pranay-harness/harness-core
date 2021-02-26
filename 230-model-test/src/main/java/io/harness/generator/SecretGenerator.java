package io.harness.generator;

import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.exception.WingsException;
import io.harness.generator.OwnerManager.Owners;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.Account;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;

@Singleton
public class SecretGenerator {
  @Inject ScmSecret scmSecret;
  @Inject SecretManager secretManager;

  public String ensureStored(String accountId, SecretName name) {
    final EncryptedData encryptedData = secretManager.getSecretByName(accountId, name.getValue());
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    SecretText secretText = SecretText.builder()
                                .name(name.getValue())
                                .value(scmSecret.decryptToString(name))
                                .usageRestrictions(getAllAppAllEnvUsageRestrictions())
                                .build();
    try {
      return secretManager.saveSecretUsingLocalMode(accountId, secretText);
    } catch (WingsException we) {
      if (we.getCause() instanceof DuplicateKeyException) {
        return secretManager.getSecretByName(accountId, name.getValue()).getUuid();
      }
      throw we;
    }
  }

  public String ensureStored(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStored(account.getUuid(), name);
  }
}
