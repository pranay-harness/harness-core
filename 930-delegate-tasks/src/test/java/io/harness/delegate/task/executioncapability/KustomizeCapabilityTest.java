/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KustomizeCapabilityTest extends CategoryTest {
  private KustomizeCapability capability =
      KustomizeCapability.builder().pluginRootDir("/home/kustomize_plugins/").build();

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getCapabilityType() {
    assertThat(capability.getCapabilityType()).isEqualTo(CapabilityType.KUSTOMIZE);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchCapabilityBasis() {
    assertThat(capability.fetchCapabilityBasis()).isEqualTo("kustomizePluginDir:/home/kustomize_plugins/");
  }
}
