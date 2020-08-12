package software.wings.service.impl.security;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.security.encryption.EncryptionType.VAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DecryptableEntity;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.ng.core.BaseNGAccess;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.NGSecretManagerService;
import software.wings.service.intfc.security.NGSecretServiceImpl;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;

import java.util.List;
import java.util.Optional;

public class NGSecretServiceImplTest extends WingsBaseTest {
  @Mock private NGSecretManagerService ngSecretManagerService;
  @Mock private SecretManager secretManager;
  @Mock private VaultService vaultService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  private static final String ACCOUNT = "Account";
  private static final String IDENTIFIER = "Account";
  private NGSecretServiceImpl ngSecretService;

  @Before
  public void setup() {
    ngSecretService = spy(new NGSecretServiceImpl(
        ngSecretManagerService, secretManager, vaultService, wingsPersistence, secretManagerConfigService));
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
    when(vaultService.encrypt(any(), any(), any(), any(), any(), any())).thenReturn(encryptedData);

    EncryptedData savedData = ngSecretService.createSecretText(secretTextDTO);
    assertThat(savedData.getName()).isEqualTo(encryptedData.getName());
    verify(secretManager).validateSecretPath(any(), anyString());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(vaultService).encrypt(any(), any(), any(), any(), any(), any());
    verify(secretManager).saveEncryptedData(any());
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
    when(vaultService.encrypt(any(), any(), any(), any(), any(), any())).thenReturn(encryptedData);

    boolean success = ngSecretService.updateSecretText(ACCOUNT, null, null, IDENTIFIER, secretTextUpdateDTO);
    assertThat(success).isTrue();
    verify(secretManager).validateSecretPath(any(), anyString());
    verify(secretManagerConfigService).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
    verify(vaultService).encrypt(any(), any(), any(), any(), any(), any());
    verify(ngSecretService).deleteSecretInSecretManager(any(), any(), any());
    verify(secretManager).saveEncryptedData(any());
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
    verify(secretManagerConfigService, times(3)).decryptEncryptionConfigSecrets(any(), any(), anyBoolean());
  }
}
