package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.AlertService;

import java.util.Optional;

public class RuntimeCredentialsInjectorTest extends CategoryTest {
  @Mock private AlertService alertService;
  @InjectMocks @Inject private VaultServiceImpl vaultService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateRuntimeParameters_shouldSucceed() {
    SecretManagerConfig vaultConfig = VaultConfig.builder().build();
    VaultServiceImpl spyVaultService = spy(vaultService);
    vaultConfig.setTemplatizedFields(Lists.newArrayList("authToken"));

    doReturn("vaultId").when(spyVaultService).updateVaultConfig(any(), any(), anyBoolean());

    Optional<SecretManagerConfig> updatedVaultConfig =
        spyVaultService.updateRuntimeCredentials(vaultConfig, Maps.newHashMap(ImmutableMap.of("authToken", "abcde")));

    assertThat(updatedVaultConfig).isPresent();
    verify(spyVaultService).updateVaultConfig(any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testUpdateRuntimeParameters_shouldFailDueToSecretManagerNotTemplatized() {
    SecretManagerConfig vaultConfig = VaultConfig.builder().build();
    VaultServiceImpl spyVaultService = spy(vaultService);

    Optional<SecretManagerConfig> updatedVaultConfig =
        spyVaultService.updateRuntimeCredentials(vaultConfig, Maps.newHashMap(ImmutableMap.of("authToken", "abcde")));
    assertThat(updatedVaultConfig).isNotPresent();
    verify(spyVaultService, times(0)).updateVaultConfig(any(), any(), anyBoolean());

    vaultConfig.setTemplatizedFields(Lists.newArrayList("authRole"));
    updatedVaultConfig = spyVaultService.updateRuntimeCredentials(vaultConfig, null);
    assertThat(updatedVaultConfig).isNotPresent();
    verify(spyVaultService, times(0)).updateVaultConfig(any(), any(), anyBoolean());

    updatedVaultConfig = spyVaultService.updateRuntimeCredentials(vaultConfig, Maps.newHashMap());
    assertThat(updatedVaultConfig).isNotPresent();
    verify(spyVaultService, times(0)).updateVaultConfig(any(), any(), anyBoolean());

    vaultConfig.setTemplatizedFields(Lists.newArrayList("appRoleId", "secretId"));
    updatedVaultConfig =
        spyVaultService.updateRuntimeCredentials(vaultConfig, Maps.newHashMap(ImmutableMap.of("authToken", "abc")));
    assertThat(updatedVaultConfig).isNotPresent();
    verify(spyVaultService, times(0)).updateVaultConfig(any(), any(), anyBoolean());
  }
}
