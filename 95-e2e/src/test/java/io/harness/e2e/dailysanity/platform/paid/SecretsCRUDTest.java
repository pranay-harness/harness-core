package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.SecretsUtils;
import io.harness.testframework.restutils.SecretsRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;

import java.util.List;

@Slf4j
public class SecretsCRUDTest extends AbstractE2ETest {
  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
  public void secretsTextCRUDTests() {
    logger.info("Local secrets text test starts");
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

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
  public void secretsFileCRUDTests() {
    logger.info("Local secrets file test starts");
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

  @Test
  @Owner(emails = SWAMY)
  @Category(E2ETests.class)
  public void secretsTextCRUDTestsWithUsageRestrictions() {
    logger.info("Local secrets text test starts");
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
}
