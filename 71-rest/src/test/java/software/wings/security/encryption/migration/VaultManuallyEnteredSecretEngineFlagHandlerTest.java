package software.wings.security.encryption.migration;

import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.VaultService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PersistenceIteratorFactory.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class VaultManuallyEnteredSecretEngineFlagHandlerTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Mock VaultService vaultService;
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject @InjectMocks VaultManuallyEnteredSecretEngineFlagHandler vaultManuallyEnteredSecretEngineFlagHandler;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    vaultManuallyEnteredSecretEngineFlagHandler.registerIterators();
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(SecretManagerConfig.class), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSetManuallyEnteredToTrue_permissionNotPresent() {
    VaultConfig vaultConfig = getVaultConfigWithAppRole("appRoleId", "secretId");
    String id = wingsPersistence.save(vaultConfig);
    vaultConfig.setUuid(id);
    when(vaultService.listSecretEngines(vaultConfig))
        .thenThrow(new SecretManagementDelegateException(VAULT_OPERATION_ERROR, "Vault cannot list engines", USER));
    vaultManuallyEnteredSecretEngineFlagHandler.handle(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, id);
    assertThat(savedVaultConfig.isEngineManuallyEntered()).isEqualTo(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSetManuallyEnteredToTrue_emptySecretEnginesList() {
    VaultConfig vaultConfig = getVaultConfigWithAppRole("appRoleId", "secretId");
    String id = wingsPersistence.save(vaultConfig);
    vaultConfig.setUuid(id);
    when(vaultService.listSecretEngines(vaultConfig)).thenReturn(new ArrayList<>());
    vaultManuallyEnteredSecretEngineFlagHandler.handle(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, id);
    assertThat(savedVaultConfig.isEngineManuallyEntered()).isEqualTo(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSetManuallyEnteredToTrue_secretEngineNotInList() {
    VaultConfig vaultConfig = getVaultConfigWithAppRole("appRoleId", "secretId");
    String id = wingsPersistence.save(vaultConfig);
    vaultConfig.setUuid(id);
    when(vaultService.listSecretEngines(vaultConfig))
        .thenReturn(Collections.singletonList(SecretEngineSummary.builder().name("harness").version(2).build()));
    vaultManuallyEnteredSecretEngineFlagHandler.handle(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, id);
    assertThat(savedVaultConfig.isEngineManuallyEntered()).isEqualTo(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSetManuallyEnteredToTrue_secretEngineVersionDifferent() {
    VaultConfig vaultConfig = getVaultConfigWithAppRole("appRoleId", "secretId");
    String id = wingsPersistence.save(vaultConfig);
    vaultConfig.setUuid(id);
    when(vaultService.listSecretEngines(vaultConfig))
        .thenReturn(Collections.singletonList(SecretEngineSummary.builder().name("secret").version(1).build()));
    vaultManuallyEnteredSecretEngineFlagHandler.handle(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, id);
    assertThat(savedVaultConfig.isEngineManuallyEntered()).isEqualTo(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSetManuallyEnteredToFalse() {
    VaultConfig vaultConfig = getVaultConfigWithAppRole("appRoleId", "secretId");
    String id = wingsPersistence.save(vaultConfig);
    vaultConfig.setUuid(id);
    when(vaultService.listSecretEngines(vaultConfig))
        .thenReturn(Collections.singletonList(SecretEngineSummary.builder().name("secret").version(2).build()));
    vaultManuallyEnteredSecretEngineFlagHandler.handle(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, id);
    assertThat(savedVaultConfig.isEngineManuallyEntered()).isEqualTo(false);
  }
}
