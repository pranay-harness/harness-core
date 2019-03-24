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
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.ManagerDecryptionService;

import java.util.List;

public class SecretsLocalFunctionalTest extends AbstractFunctionalTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretsLocalFunctionalTest.class);
  @Inject private ManagerDecryptionService managerDecryptionService;
  SecretsRestUtils srUtils = new SecretsRestUtils();

  @Test
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void secretsCRUDTests() {
    logger.info("Local secrets test starts");
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
    assertTrue(encryptedDataList.size() == 0);
  }
}
