/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
