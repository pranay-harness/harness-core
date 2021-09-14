/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.kustomize;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KustomizeConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneFromNull() {
    KustomizeConfig sourceConfig = null;
    KustomizeConfig destConfig = KustomizeConfig.cloneFrom(sourceConfig);

    assertThat(destConfig).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cloneFromValidConfig() {
    KustomizeConfig sourceConfig = KustomizeConfig.builder().pluginRootDir("/home/wings/").build();
    KustomizeConfig destConfig = KustomizeConfig.cloneFrom(sourceConfig);

    assertThat(sourceConfig != destConfig).isTrue();
    assertThat(destConfig).isEqualTo(sourceConfig);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void kustomizeDirPathShouldNotBeNull() {
    KustomizeConfig config = new KustomizeConfig();
    assertThat(config.getKustomizeDirPath()).isNotNull();
    assertThat(config.getKustomizeDirPath()).isEmpty();

    config = KustomizeConfig.builder().build();
    assertThat(config.getKustomizeDirPath()).isNotNull();
    assertThat(config.getKustomizeDirPath()).isEmpty();
  }
}
