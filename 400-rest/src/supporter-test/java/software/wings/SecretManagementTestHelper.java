package software.wings;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.exception.SecretManagementException;
import io.harness.security.encryption.EncryptionType;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.LocalSecretManagerService;

import com.google.inject.Inject;
import java.util.Objects;

public class SecretManagementTestHelper {
  @Inject private LocalSecretManagerService localSecretManagerService;

  public static boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig) {
    if (Objects.equals(cyberArkConfig.getCyberArkUrl(), "invalidUrl")) {
      throw new SecretManagementException("Invalid Url");
    }
    if (Objects.equals(cyberArkConfig.getClientCertificate(), "invalidCertificate")) {
      throw new SecretManagementException("Invalid credentials");
    }
    return true;
  }

  public VaultConfig getCommonVaultConfig() {
    VaultConfig vaultConfig =
        VaultConfig.builder().vaultUrl("http://127.0.0.1:8200").name("myVault").secretEngineVersion(1).build();
    vaultConfig.setDefault(true);
    vaultConfig.setReadOnly(false);
    vaultConfig.setEncryptionType(EncryptionType.VAULT);
    vaultConfig.setSecretEngineName("secret");
    vaultConfig.setSecretEngineVersion(2);
    vaultConfig.setUsageRestrictions(
        localSecretManagerService.getEncryptionConfig(generateUuid()).getUsageRestrictions());
    return vaultConfig;
  }

  public VaultConfig getVaultConfigWithAuthToken() {
    return getVaultConfigWithAuthToken(generateUuid());
  }

  public VaultConfig getVaultConfigWithAuthToken(String authToken) {
    VaultConfig vaultConfig = getCommonVaultConfig();
    vaultConfig.setAuthToken(authToken);
    return vaultConfig;
  }

  public VaultConfig getVaultConfigWithAppRole(String appRoleId, String secretId) {
    VaultConfig vaultConfig = getCommonVaultConfig();
    vaultConfig.setAppRoleId(appRoleId);
    vaultConfig.setSecretId(secretId);
    return vaultConfig;
  }

  public KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    kmsConfig.setEncryptionType(EncryptionType.KMS);
    kmsConfig.setUsageRestrictions(
        localSecretManagerService.getEncryptionConfig(generateUuid()).getUsageRestrictions());
    return kmsConfig;
  }

  public GcpKmsConfig getGcpKmsConfig() {
    GcpKmsConfig gcpKmsConfig =
        new GcpKmsConfig("gcpKms", "projectId", "region", "keyRing", "keyName", "{\"abc\": \"value\"}".toCharArray());
    gcpKmsConfig.setDefault(true);
    gcpKmsConfig.setEncryptionType(EncryptionType.GCP_KMS);
    gcpKmsConfig.setUsageRestrictions(
        localSecretManagerService.getEncryptionConfig(generateUuid()).getUsageRestrictions());
    return gcpKmsConfig;
  }

  public CyberArkConfig getCyberArkConfig() {
    return getCyberArkConfig(null);
  }

  public CyberArkConfig getCyberArkConfig(String clientCertificate) {
    final CyberArkConfig cyberArkConfig = new CyberArkConfig();
    cyberArkConfig.setName("myCyberArk");
    cyberArkConfig.setDefault(false);
    cyberArkConfig.setCyberArkUrl("https://app.harness.io"); // Just a valid URL.
    cyberArkConfig.setAppId(generateUuid());
    cyberArkConfig.setClientCertificate(clientCertificate);
    cyberArkConfig.setEncryptionType(EncryptionType.CYBERARK);
    cyberArkConfig.setUsageRestrictions(
        localSecretManagerService.getEncryptionConfig(generateUuid()).getUsageRestrictions());
    return cyberArkConfig;
  }

  public static AzureVaultConfig getAzureVaultConfig() {
    AzureVaultConfig azureVaultConfig = new AzureVaultConfig();
    azureVaultConfig.setName("myAzureVault");
    azureVaultConfig.setSecretKey(generateUuid());
    azureVaultConfig.setDefault(true);
    azureVaultConfig.setVaultName(generateUuid());
    azureVaultConfig.setClientId(generateUuid());
    azureVaultConfig.setSubscription(generateUuid());
    azureVaultConfig.setTenantId(generateUuid());
    azureVaultConfig.setEncryptionType(EncryptionType.AZURE_VAULT);
    return azureVaultConfig;
  }

  public static AwsSecretsManagerConfig getAwsSecretManagerConfig() {
    AwsSecretsManagerConfig secretsManagerConfig = AwsSecretsManagerConfig.builder()
                                                       .name("myAwsSecretManager")
                                                       .accessKey(generateUuid())
                                                       .secretKey(generateUuid())
                                                       .region("us-east-1")
                                                       .secretNamePrefix(generateUuid())
                                                       .build();
    secretsManagerConfig.setDefault(true);
    secretsManagerConfig.setEncryptionType(EncryptionType.AWS_SECRETS_MANAGER);

    return secretsManagerConfig;
  }
}
