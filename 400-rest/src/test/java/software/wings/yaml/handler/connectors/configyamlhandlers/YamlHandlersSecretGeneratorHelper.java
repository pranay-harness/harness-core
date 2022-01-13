/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
