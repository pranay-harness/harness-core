/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.secrets;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretText;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class SecretsLocalFunctionalTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = NATARAJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void secretsTextCRUDTests() {
    log.info("Local secrets text test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText = SecretsUtils.createSecretTextObject(secretsName, secretValue);
    List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isFalse();

    String secretsId = SecretsRestUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
    assertThat(StringUtils.isNotBlank(secretsId)).isTrue();

    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isTrue();
    secretText.setName(secretsNewName);

    boolean isUpdationDone = SecretsRestUtils.updateSecret(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertThat(isUpdationDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isTrue();

    // Decryption is coupled with EncryptedSettings. Hence individual encryptedSetting test should involve a decryption
    // test
    EncryptedData data = encryptedDataList.get(0);
    assertThat(data).isNotNull();

    boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertThat(isDeletionDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isFalse();
  }

  /*
   * Ignored because it was causing RBAC errors.
   * TO-DO: Work with Rama to have this resolved
   */
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("TODO: Ignored because it was causing RBAC errors when running in parallel")
  public void secretsTextCRUDTestsWithUsageRestrictions() {
    log.info("Local secrets text test starts");
    String secretsName = "AnotherSecret-" + System.currentTimeMillis();
    String secretsNewName = "AnotherNewName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText =
        SecretsUtils.createSecretTextObjectWithUsageRestriction(secretsName, secretValue, "NON_PROD");
    List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isFalse();
    String secretsId = SecretsRestUtils.addSecretWithUsageRestrictions(getAccount().getUuid(), bearerToken, secretText);
    assertThat(StringUtils.isNotBlank(secretsId)).isTrue();

    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isTrue();
    secretText.setName(secretsNewName);

    boolean isUpdationDone =
        SecretsRestUtils.updateSecretWithUsageRestriction(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertThat(isUpdationDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isTrue();

    // Decryption is coupled with EncryptedSettings. Hence individual encryptedSetting test should involve a decryption
    // test
    EncryptedData data = encryptedDataList.get(0);
    assertThat(data).isNotNull();

    boolean isDeletionDone = SecretsRestUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertThat(isDeletionDone).isTrue();
    encryptedDataList = SecretsRestUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isFalse();
  }

  @Test
  @Owner(developers = NATARAJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void secretsFileCRUDTests() {
    log.info("Local secrets file test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "testFile.txt";

    List<EncryptedData> encryptedDataList = SecretsRestUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isFalse();

    String secretId = SecretsRestUtils.addSecretFile(getAccount().getUuid(), bearerToken, secretsName, filePath);
    assertThat(StringUtils.isNotBlank(secretId)).isTrue();
    encryptedDataList = SecretsRestUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertThat(isSecretPresent).isTrue();

    boolean isUpdated =
        SecretsRestUtils.updateSecretFile(getAccount().getUuid(), bearerToken, secretsNewName, filePath, secretId);
    assertThat(isUpdated).isTrue();
    encryptedDataList = SecretsRestUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isTrue();

    boolean isDeleted = SecretsRestUtils.deleteSecretFile(getAccount().getUuid(), bearerToken, secretId);
    assertThat(isDeleted).isTrue();
    encryptedDataList = SecretsRestUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertThat(isSecretPresent).isFalse();
  }
}
