package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.SecretManagementException;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secrets.SecretsFileService;

import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.NGSecretFileServiceImpl;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.NGSecretService;

import java.util.ArrayList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class NGSecretFileServiceImplTest extends WingsBaseTest {
  @Mock private NGSecretManagerService ngSecretManagerService;
  @Mock private NGSecretService ngSecretService;
  @Mock private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Mock private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Mock private SecretsFileService secretsFileService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private FileService fileService;
  @Mock private VaultEncryptor vaultEncryptor;
  @Mock private KmsEncryptor kmsEncryptor;
  private NGSecretFileServiceImpl ngSecretFileService;
  private static final String ACCOUNT = "Account";
  private static final String IDENTIFIER = "Account";
  private static final String SECRET_MANAGER = "Account";
  private static final String NAME = "Name";

  @Before
  public void setup() {
    ngSecretFileService =
        spy(new NGSecretFileServiceImpl(ngSecretManagerService, ngSecretService, vaultEncryptorsRegistry,
            kmsEncryptorsRegistry, secretsFileService, wingsPersistence, secretManagerConfigService, fileService));
    when(vaultEncryptorsRegistry.getVaultEncryptor(any())).thenReturn(vaultEncryptor);
    when(kmsEncryptorsRegistry.getKmsEncryptor(any())).thenReturn(kmsEncryptor);
  }

  private SecretFileDTO getSecretFileDTO() {
    return SecretFileDTO.builder()
        .account(ACCOUNT)
        .secretManager(SECRET_MANAGER)
        .identifier(IDENTIFIER)
        .name(NAME)
        .type(SecretType.SecretFile)
        .build();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretFile() {
    SecretFileDTO secretFileDTO = getSecretFileDTO();
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    secretManagerConfig.setEncryptionType(VAULT);
    ((VaultConfig) secretManagerConfig).setReadOnly(false);
    EncryptedData encryptedData = random(EncryptedData.class);

    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(ngSecretManagerService.get(any(), any(), any(), any())).thenReturn(Optional.ofNullable(secretManagerConfig));
    doNothing().when(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);
    EncryptedData savedData = ngSecretFileService.create(secretFileDTO, null);

    assertThat(savedData.getName()).isEqualTo(secretFileDTO.getName());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(wingsPersistence).save(any(EncryptedData.class));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretFileShouldFail_secretAlreadyExists() {
    EncryptedData encryptedData = random(EncryptedData.class);

    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.of(encryptedData));
    try {
      ngSecretFileService.create(random(SecretFileDTO.class), null);
      fail("Creation of secret file should fail.");
    } catch (SecretManagementException secretManagementException) {
      // nothing required
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretShouldFail_SecretManagerAbsent() {
    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    when(ngSecretManagerService.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    try {
      ngSecretFileService.create(random(SecretFileDTO.class), null);
      fail("Creation of secret file should fail.");
    } catch (SecretManagementException secretManagementException) {
      // nothing required
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecret() {
    EncryptedData encryptedData = random(EncryptedData.class);
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    secretManagerConfig.setEncryptionType(VAULT);
    ((VaultConfig) secretManagerConfig).setReadOnly(false);
    SecretFileUpdateDTO secretFileUpdateDTO = SecretFileUpdateDTO.builder()
                                                  .name(encryptedData.getName())
                                                  .description("random")
                                                  .tags(new ArrayList<>())
                                                  .build();
    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.ofNullable(encryptedData));
    when(ngSecretManagerService.get(any(), any(), any(), any())).thenReturn(Optional.of(secretManagerConfig));
    doNothing().when(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);
    when(vaultEncryptor.updateSecret(any(), any(), any(), any(), any())).thenReturn(encryptedData);
    boolean success = ngSecretFileService.update(ACCOUNT, null, null, IDENTIFIER, secretFileUpdateDTO, null);
    assertThat(success).isEqualTo(true);
    verify(ngSecretService, times(0)).deleteSecretInSecretManager(any(), any(), any());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);
    verify(wingsPersistence).save(any(EncryptedData.class));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecretWithNameChanged() {
    EncryptedData encryptedData = random(EncryptedData.class);
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    secretManagerConfig.setEncryptionType(VAULT);
    ((VaultConfig) secretManagerConfig).setReadOnly(false);
    SecretFileUpdateDTO secretFileUpdateDTO =
        SecretFileUpdateDTO.builder().name(random(String.class)).description("random").tags(new ArrayList<>()).build();
    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.ofNullable(encryptedData));
    when(ngSecretManagerService.get(any(), any(), any(), any())).thenReturn(Optional.of(secretManagerConfig));
    doNothing().when(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);
    when(vaultEncryptor.updateSecret(any(), any(), any(), any(), any())).thenReturn(encryptedData);
    boolean success = ngSecretFileService.update(ACCOUNT, null, null, IDENTIFIER, secretFileUpdateDTO, null);
    assertThat(success).isEqualTo(true);
    verify(ngSecretService, times(1)).deleteSecretInSecretManager(any(), any(), any());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(wingsPersistence).save(any(EncryptedData.class));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecretShouldFail_SecretDoesNotExist() {
    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    try {
      ngSecretFileService.update(ACCOUNT, null, null, IDENTIFIER, random(SecretFileUpdateDTO.class), null);
      fail("Updating of secret file should fail.");
    } catch (SecretManagementException exception) {
      // do nothing
    }
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecretShouldFail_SecretManagerDoesNotExist() {
    when(ngSecretService.get(any(), any(), any(), any())).thenReturn(Optional.of(random(EncryptedData.class)));
    when(ngSecretManagerService.get(any(), any(), any(), any())).thenReturn(Optional.empty());
    try {
      ngSecretFileService.update(ACCOUNT, null, null, IDENTIFIER, random(SecretFileUpdateDTO.class), null);
      fail("Updating of secret file should fail.");
    } catch (SecretManagementException exception) {
      // do nothing
    }
  }
}
