/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretFile;
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

  private String ensureSecretText(String accountId, String name, String value) {
    final EncryptedData encryptedData = secretManager.getSecretByName(accountId, name);
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    SecretText secretText =
        SecretText.builder().name(name).value(value).usageRestrictions(getAllAppAllEnvUsageRestrictions()).build();
    try {
      return secretManager.saveSecretUsingLocalMode(accountId, secretText);
    } catch (WingsException we) {
      if (we.getCause() instanceof DuplicateKeyException) {
        return secretManager.getSecretByName(accountId, name).getUuid();
      }
      throw we;
    }
  }

  private String ensureSecretFile(String accountId, String name, String value) {
    final EncryptedData encryptedData = secretManager.getSecretByName(accountId, name);
    if (encryptedData != null) {
      return encryptedData.getUuid();
    }

    SecretFile secretFile = SecretFile.builder()
                                .name(name)
                                .fileContent(value.getBytes())
                                .usageRestrictions(getAllAppAllEnvUsageRestrictions())
                                .build();
    try {
      return secretManager.saveSecretFile(accountId, secretFile);
    } catch (WingsException we) {
      if (we.getCause() instanceof DuplicateKeyException) {
        return secretManager.getSecretByName(accountId, name).getUuid();
      }
      throw we;
    }
  }

  public String ensureSecretText(Owners owners, String name, String value) {
    final Account account = owners.obtainAccount();
    return ensureSecretText(account.getUuid(), name, value);
  }

  public String ensureStored(String accountId, SecretName name) {
    return ensureSecretText(accountId, name.getValue(), scmSecret.decryptToString(name));
  }

  private String ensureStoredFile(String accountId, SecretName name) {
    return ensureSecretFile(accountId, name.getValue(), scmSecret.decryptToString(name));
  }

  public String ensureStored(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStored(account.getUuid(), name);
  }

  public String ensureStoredFile(Owners owners, SecretName name) {
    final Account account = owners.obtainAccount();
    return ensureStoredFile(account.getUuid(), name);
  }
}
