/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VaultConfigRenewalIntervalMigrationTest extends WingsBaseTest {
  @Inject private VaultConfigRenewalIntervalMigration vaultConfigRenewalIntervalMigration;
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrate_renewalIntervalIsZero() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").authToken("authToken").renewIntervalHours(0).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = persistence.save(vaultConfig);
    vaultConfigRenewalIntervalMigration.migrate();
    VaultConfig returnedVaultConfig = persistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrate_renewalIntervalIsNonZero() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").authToken("authToken").renewIntervalHours(2).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = persistence.save(vaultConfig);
    vaultConfigRenewalIntervalMigration.migrate();
    VaultConfig returnedVaultConfig = persistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(Duration.ofHours(2).toMinutes());
  }
}
