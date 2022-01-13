/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.security.SimpleEncryption.CHARSET;

import io.harness.beans.SecretFile;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;

import software.wings.beans.Account;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;

public class EncryptedFileHelper {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  private String secretName = "secretName";
  private String secret = "secret";
  @Inject SecretManager secretManager;

  public String createEncryptedFile(String name) {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    SecretFile secretFile = SecretFile.builder().name(name).fileContent("test".getBytes(CHARSET)).build();
    return secretManager.saveSecretFile(accountId, secretFile);
  }
}
