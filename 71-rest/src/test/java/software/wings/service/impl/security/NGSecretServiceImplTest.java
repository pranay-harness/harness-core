package software.wings.service.impl.security;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.security.encryption.EncryptionType.VAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.KmsEncryptorsRegistry;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.ng.core.BaseNGAccess;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.NGSecretServiceImpl;

import java.util.List;
import java.util.Optional;

public class NGSecretServiceImplTest extends WingsBaseTest {
  @Mock private NGSecretManagerService ngSecretManagerService;
  @Mock private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Mock private KmsEncryptorsRegistry kmsEncryptorsRegistry;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private FileService fileService;
  @Mock private VaultEncryptor vaultEncryptor;
  @Mock private KmsEncryptor kmsEncryptor;
  private static final String ACCOUNT = "Account";
  private static final String IDENTIFIER = "Account";
  private NGSecretServiceImpl ngSecretService;

  @Before
  public void setup() {
    ngSecretService = spy(new NGSecretServiceImpl(vaultEncryptorsRegistry, kmsEncryptorsRegistry,
        ngSecretManagerService, wingsPersistence, fileService, secretManagerConfigService));
    when(vaultEncryptorsRegistry.getVaultEncryptor(any())).thenReturn(vaultEncryptor);
    when(kmsEncryptorsRegistry.getKmsEncryptor(any())).thenReturn(kmsEncryptor);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecret() {
    SecretTextDTO secretTextDTO = random(SecretTextDTO.class);
    secretTextDTO.setPath(null);
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    EncryptedData encryptedData = random(EncryptedData.class);
    encryptedData.setEncryptionType(VAULT);

    doReturn(Optional.empty()).when(ngSecretService).get(any(), any(), any(), any());
    when(ngSecretManagerService.getSecretManager(any(), any(), any(), any()))
        .thenReturn(Optional.of(secretManagerConfig));
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);

    EncryptedData savedData = ngSecretService.createSecretText(secretTextDTO);
    assertThat(savedData.getName()).isEqualTo(secretTextDTO.getName());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(vaultEncryptor).createSecret(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateSecret() {
    SecretTextUpdateDTO secretTextUpdateDTO = random(SecretTextUpdateDTO.class);
    secretTextUpdateDTO.setPath(null);
    EncryptedData encryptedData = random(EncryptedData.class);
    encryptedData.setPath(null);
    encryptedData.setEncryptionType(VAULT);
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    secretManagerConfig.setEncryptionType(VAULT);

    doReturn(Optional.of(encryptedData)).when(ngSecretService).get(any(), any(), any(), any());
    when(ngSecretManagerService.getSecretManager(any(), any(), any(), any()))
        .thenReturn(Optional.of(secretManagerConfig));
    doNothing().when(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    doNothing().when(ngSecretService).deleteSecretInSecretManager(any(), any(), any());
    when(vaultEncryptor.createSecret(any(), any(), any(), any())).thenReturn(encryptedData);

    boolean success = ngSecretService.updateSecretText(ACCOUNT, null, null, IDENTIFIER, secretTextUpdateDTO);
    assertThat(success).isTrue();
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(vaultEncryptor).createSecret(any(), any(), any(), any());
    verify(ngSecretService).deleteSecretInSecretManager(any(), any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testGetEncryptionDetails() {
    EncryptedData encryptedData = random(EncryptedData.class);
    SecretManagerConfig secretManagerConfig = random(VaultConfig.class);
    DecryptableEntity decryptableEntity = random(KubernetesClientKeyCertDTO.class);
    decryptableEntity.setDecrypted(false);
    doReturn(Optional.of(encryptedData)).when(ngSecretService).get(any(), any(), any(), any());
    when(ngSecretManagerService.getSecretManager(any(), any(), any(), any()))
        .thenReturn(Optional.of(secretManagerConfig));
    doNothing().when(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());

    List<EncryptedDataDetail> detailList =
        ngSecretService.getEncryptionDetails(random(BaseNGAccess.class), decryptableEntity);
    assertThat(detailList).isNotEmpty();
    verify(secretManagerConfigService, times(4)).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
  }
}
