/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.setupusage;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.SMCoreTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

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
