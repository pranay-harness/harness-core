package io.harness.functional.secrets;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.RestUtils.SecretsRestUtils;
import io.harness.Utils.SecretsUtils;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.ManagerDecryptionService;

import java.util.List;

@Slf4j
public class SecretsLocalFunctionalTest extends AbstractFunctionalTest {
  @Inject private ManagerDecryptionService managerDecryptionService;
  SecretsRestUtils srUtils = new SecretsRestUtils();

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void secretsTextCRUDTests() {
    logger.info("Local secrets text test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText = SecretsUtils.createSecretTextObject(secretsName, secretValue);
    List<EncryptedData> encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertFalse(isSecretPresent);

    String secretsId = srUtils.addSecret(getAccount().getUuid(), bearerToken, secretText);
    assertTrue(StringUtils.isNotBlank(secretsId));

    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertTrue(isSecretPresent);
    secretText.setName(secretsNewName);

    boolean isUpdationDone = srUtils.updateSecret(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertTrue(isUpdationDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertTrue(isSecretPresent);

    // Decryption is coupled with EncryptedSettings. Hence individual encryptedSetting test should involve a decryption
    // test
    EncryptedData data = encryptedDataList.get(0);
    assertNotNull(data);

    boolean isDeletionDone = srUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertTrue(isDeletionDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertFalse(isSecretPresent);
  }

  /*
   * Ignored because it was causing RBAC errors.
   * TO-DO: Work with Rama to have this resolved
   */
  @Test
  @Owner(emails = "swamy@harness.io")
  @Category(FunctionalTests.class)
  @Ignore
  public void secretsTextCRUDTestsWithUsageRestrictions() {
    logger.info("Local secrets text test starts");
    String secretsName = "AnotherSecret-" + System.currentTimeMillis();
    String secretsNewName = "AnotherNewName-" + System.currentTimeMillis();
    String secretValue = "value";

    SecretText secretText =
        SecretsUtils.createSecretTextObjectWithUsageRestriction(secretsName, secretValue, "NON_PROD");
    List<EncryptedData> encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertFalse(isSecretPresent);
    String secretsId = srUtils.addSecretWithUsageRestrictions(getAccount().getUuid(), bearerToken, secretText);
    assertTrue(StringUtils.isNotBlank(secretsId));

    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertTrue(isSecretPresent);
    secretText.setName(secretsNewName);

    boolean isUpdationDone =
        srUtils.updateSecretWithUsageRestriction(getAccount().getUuid(), bearerToken, secretsId, secretText);
    assertTrue(isUpdationDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertTrue(isSecretPresent);

    // Decryption is coupled with EncryptedSettings. Hence individual encryptedSetting test should involve a decryption
    // test
    EncryptedData data = encryptedDataList.get(0);
    assertNotNull(data);

    boolean isDeletionDone = srUtils.deleteSecret(getAccount().getUuid(), bearerToken, secretsId);
    assertTrue(isDeletionDone);
    encryptedDataList = srUtils.listSecrets(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertFalse(isSecretPresent);
  }

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void secretsFileCRUDTests() {
    logger.info("Local secrets file test starts");
    String secretsName = "Secret-" + System.currentTimeMillis();
    String secretsNewName = "newName-" + System.currentTimeMillis();
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "testFile.txt";

    List<EncryptedData> encryptedDataList = srUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    boolean isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertFalse(isSecretPresent);

    String secretId = srUtils.addSecretFile(getAccount().getUuid(), bearerToken, secretsName, filePath);
    assertTrue(StringUtils.isNotBlank(secretId));
    encryptedDataList = srUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsName);
    assertTrue(isSecretPresent);

    boolean isUpdated =
        srUtils.updateSecretFile(getAccount().getUuid(), bearerToken, secretsNewName, filePath, secretId);
    assertTrue(isUpdated);
    encryptedDataList = srUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertTrue(isSecretPresent);

    boolean isDeleted = srUtils.deleteSecretFile(getAccount().getUuid(), bearerToken, secretId);
    assertTrue(isDeleted);
    encryptedDataList = srUtils.listSecretsFile(getAccount().getUuid(), bearerToken);
    isSecretPresent = SecretsUtils.isSecretAvailable(encryptedDataList, secretsNewName);
    assertFalse(isSecretPresent);
  }
}
