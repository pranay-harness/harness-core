/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secret;

import static io.harness.rule.OwnerRule.ABHINAV;

import static software.wings.graphql.schema.type.secrets.QLSecretType.ENCRYPTED_FILE;
import static software.wings.graphql.schema.type.secrets.QLSecretType.ENCRYPTED_TEXT;
import static software.wings.graphql.schema.type.secrets.QLSecretType.SSH_CREDENTIAL;
import static software.wings.graphql.schema.type.secrets.QLSecretType.WINRM_CREDENTIAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetSecretTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject SettingsService settingsService;
  @Inject SecretManager secretManager;
  @Inject AccountGenerator accountGenerator;
  @Inject SSHCredentialHelper sshCredentialHelper;
  @Inject EncryptedTextHelper encryptedTextHelper;

  @Inject WinRMCredentialHelper winRMCredentialHelper;
  @Inject EncryptedFileHelper encryptedFileHelper;

  @Inject WingsPersistence wingsPersistence;
  String encryptedTextSecretName = "encrypted text";
  String encryptedFileSecretName = "encrypted file";
  String winrmSecretName = "winrm cred";
  String sshSecretName = "ssh cred";

  Account account;

  @Before
  public void setup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretByNameQuery() {
    String secretQueryPattern = MultilineStringMixin.$.GQL(/*
{
secretByName(name:"%s", secretType:%s){
secretType
name
id
}
}
*/ GetSecretTest.class);
    saveSecrets();

    testExistingSecretFromApi(secretQueryPattern, encryptedTextSecretName, ENCRYPTED_TEXT);
    testExistingSecretFromApi(secretQueryPattern, encryptedFileSecretName, ENCRYPTED_FILE);
    testExistingSecretFromApi(secretQueryPattern, winrmSecretName, WINRM_CREDENTIAL);
    testExistingSecretFromApi(secretQueryPattern, sshSecretName, SSH_CREDENTIAL);

    // Test that things don't popup with wrong name.
    testSecretDoesNotExist(secretQueryPattern, encryptedFileSecretName, ENCRYPTED_TEXT);
    testSecretDoesNotExist(secretQueryPattern, winrmSecretName, SSH_CREDENTIAL);
    testSecretDoesNotExist(secretQueryPattern, sshSecretName, ENCRYPTED_FILE);
  }

  private void testExistingSecretFromApi(String secretQueryPattern, String name, QLSecretType type) {
    String query = String.format(secretQueryPattern, name, type);
    final QLTestObject qlSecretObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretObject.get("name")).isEqualTo(name);
  }

  private void testExistingSecretIdFromApi(String secretQueryPattern, String name, QLSecretType type) {
    String query = String.format(secretQueryPattern, name, type);
    final QLTestObject qlSecretObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretObject.get("id")).isEqualTo(name);
  }

  private void testSecretDoesNotExist(String secretQueryPattern, String name, QLSecretType type) {
    String query = String.format(secretQueryPattern, name, type);
    assertThatThrownBy(() -> qlExecute(query, account.getUuid())).hasMessageContaining("No secret exists");
  }

  private Map<QLSecretType, String> saveSecrets() {
    Map<QLSecretType, String> secretsUuid = new HashMap<>();
    secretsUuid.put(ENCRYPTED_TEXT, encryptedTextHelper.CreateEncryptedText(encryptedTextSecretName));
    secretsUuid.put(WINRM_CREDENTIAL, winRMCredentialHelper.createWinRMCredential(winrmSecretName));
    secretsUuid.put(SSH_CREDENTIAL, sshCredentialHelper.createSSHCredential(sshSecretName));
    secretsUuid.put(ENCRYPTED_FILE, encryptedFileHelper.createEncryptedFile(encryptedFileSecretName));
    return secretsUuid;
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretByIdQuery() {
    String secretQueryPattern = MultilineStringMixin.$.GQL(/*
{
secret(secretId:"%s", secretType:%s){
secretType
name
id
}
}
*/ GetSecretTest.class);
    Map<QLSecretType, String> secretTypeUuidMap = saveSecrets();

    for (QLSecretType type : QLSecretType.values()) {
      testExistingSecretIdFromApi(secretQueryPattern, secretTypeUuidMap.get(type), type);
    }

    // Test that things don't popup with wrong name.
    testSecretDoesNotExist(secretQueryPattern, secretTypeUuidMap.get(ENCRYPTED_FILE), ENCRYPTED_TEXT);
    testSecretDoesNotExist(secretQueryPattern, secretTypeUuidMap.get(WINRM_CREDENTIAL), SSH_CREDENTIAL);
    testSecretDoesNotExist(secretQueryPattern, secretTypeUuidMap.get(SSH_CREDENTIAL), ENCRYPTED_FILE);
  }
}
