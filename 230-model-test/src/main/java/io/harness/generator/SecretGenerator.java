package io.harness.generator;

import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.generator.OwnerManager.Owners;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.Account;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SecretGenerator {
  @Inject ScmSecret scmSecret;
  @Inject SecretManager secretManager;

  String ensureStored(String accountId, SecretName name) {
    final EncryptedData encryptedData = secretManager.getSecretByName(accountId, name.getValue());
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    SecretText secretText = SecretText.builder()
                                .name(name.getValue())
                                .value(scmSecret.decryptToString(name))
                                .usageRestrictions(getAllAppAllEnvUsageRestrictions())
                                .build();
    return secretManager.saveSecretUsingLocalMode(accountId, secretText);
  }

  public String ensureStored(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStored(account.getUuid(), name);
  }
}
