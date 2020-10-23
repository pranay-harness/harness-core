package io.harness.secrets.setupusage;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.SMCoreTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.settings.SettingVariableTypes;

import java.util.Optional;

public class SecretSetupUsageBuilderRegistryTest extends SMCoreTestBase {
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
