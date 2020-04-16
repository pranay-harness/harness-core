package software.wings.security.encryption.setupusage;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.security.encryption.setupusage.builders.ConfigFileSetupUsageBuilder;
import software.wings.security.encryption.setupusage.builders.SecretManagerSetupUsageBuilder;
import software.wings.security.encryption.setupusage.builders.ServiceVariableSetupUsageBuilder;
import software.wings.security.encryption.setupusage.builders.SettingAttributeSetupUsageBuilder;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Optional;

public class SecretSetupUsageBuilderRegistryTest extends WingsBaseTest {
  @Mock private SecretManagerSetupUsageBuilder secretManagerSetupUsageBuilder;
  @Mock private ConfigFileSetupUsageBuilder configFileSetupUsageBuilder;
  @Mock private ServiceVariableSetupUsageBuilder serviceVariableSetupUsageBuilder;
  @Mock private SettingAttributeSetupUsageBuilder settingAttributeSetupUsageBuilder;
  @Inject @InjectMocks private SecretSetupUsageBuilderRegistry secretSetupUsageBuilderRegistry;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetMigrator_shouldPass() {
    SettingVariableTypes type = SettingVariableTypes.AWS;
    Optional<SecretSetupUsageBuilder> secretSetupUsageBuilder =
        secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(type);
    assertThat(secretSetupUsageBuilder.isPresent()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetMigrator_shouldFail() {
    SettingVariableTypes type = SettingVariableTypes.PHYSICAL_DATA_CENTER;
    Optional<SecretSetupUsageBuilder> secretSetupUsageBuilder =
        secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(type);
    assertThat(secretSetupUsageBuilder.isPresent()).isFalse();
  }
}
