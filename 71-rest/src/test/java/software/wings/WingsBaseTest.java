package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.exception.KmsOperationException;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.rules.WingsRule;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.HashSet;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest extends CategoryTest implements MockableTestMixin {
  private static final String plainTextKey = "1234567890123456";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  // I am not absolutely sure why, but there is dependency between wings io.harness.rule and
  // MockitoJUnit io.harness.rule and they have to be listed in these order
  @Rule public WingsRule wingsRule = new WingsRule();

  protected EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig) throws Exception {
    if (kmsConfig.getAccessKey().equals("invalidKey")) {
      throw new KmsOperationException("Invalid credentials");
    }
    char[] encryptedValue = value == null ? null
                                          : SecretManagementDelegateServiceImpl.encrypt(
                                                new String(value), new SecretKeySpec(plainTextKey.getBytes(), "AES"));

    return EncryptedData.builder()
        .encryptionKey(plainTextKey)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.KMS)
        .kmsId(kmsConfig.getUuid())
        .enabled(true)
        .parentIds(new HashSet<>())
        .accountId(accountId)
        .build();
  }

  protected char[] decrypt(EncryptedData data, KmsConfig kmsConfig) throws Exception {
    return SecretManagementDelegateServiceImpl
        .decrypt(data.getEncryptedValue(), new SecretKeySpec(plainTextKey.getBytes(), "AES"))
        .toCharArray();
  }

  protected EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) throws IOException {
    if (vaultConfig.getAuthToken().equals("invalidKey")) {
      throw new KmsOperationException("invalidKey");
    }
    String keyUrl = settingType + "/" + name;
    if (savedEncryptedData != null) {
      savedEncryptedData.setEncryptionKey(keyUrl);
      savedEncryptedData.setEncryptedValue(value == null ? keyUrl.toCharArray() : value.toCharArray());
      return savedEncryptedData;
    }

    return EncryptedData.builder()
        .encryptionKey(keyUrl)
        .encryptedValue(value == null ? null : value.toCharArray())
        .encryptionType(EncryptionType.VAULT)
        .enabled(true)
        .accountId(accountId)
        .parentIds(new HashSet<>())
        .kmsId(vaultConfig.getUuid())
        .build();
  }

  protected char[] decrypt(EncryptedData data, VaultConfig vaultConfig) throws IOException {
    if (data.getEncryptedValue() == null) {
      return null;
    }
    return data.getEncryptedValue();
  }

  protected VaultConfig getVaultConfig() {
    return getVaultConfig(generateUuid());
  }

  protected VaultConfig getVaultConfig(String authToken) {
    return VaultConfig.builder()
        .vaultUrl("http://127.0.0.1:8200")
        .authToken(authToken)
        .name("myVault")
        .isDefault(true)
        .secretEngineVersion(1)
        .build();
  }

  protected KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    return kmsConfig;
  }
}
