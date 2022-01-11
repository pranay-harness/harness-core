/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import io.harness.beans.SecretText;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;

import software.wings.beans.Account;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;

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
    return secretManager.saveSecretText(accountId, secretText, false);
  }
}
