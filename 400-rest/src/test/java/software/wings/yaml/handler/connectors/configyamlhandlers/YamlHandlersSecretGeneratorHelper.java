/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.handler.connectors.configyamlhandlers;

import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;

public class YamlHandlersSecretGeneratorHelper {
  @Inject ScmSecret scmSecret;
  @Inject SecretManager secretManager;

  public String generateSecret(String accountId, SecretName name) {
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
}
