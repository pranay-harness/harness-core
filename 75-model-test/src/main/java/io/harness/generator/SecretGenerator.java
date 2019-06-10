package io.harness.generator;

import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.OwnerManager.Owners;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import software.wings.beans.Account;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

@Singleton
public class SecretGenerator {
  @Inject ScmSecret scmSecret;
  @Inject SecretManager secretManager;

  String ensureStored(String accountId, SecretName name) {
    final EncryptedData encryptedData = secretManager.getSecretByName(accountId, name.getValue());
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    return secretManager.saveSecret(
        accountId, name.getValue(), scmSecret.decryptToString(name), null, getAllAppAllEnvUsageRestrictions());
  }

  public String ensureStored(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStored(account.getUuid(), name);
  }
}
