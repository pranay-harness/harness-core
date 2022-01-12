/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helm;

import static io.harness.helm.HelmCliCommandType.DELETE_RELEASE;
import static io.harness.helm.HelmCliCommandType.INSTALL;
import static io.harness.helm.HelmCliCommandType.RENDER_CHART;
import static io.harness.helm.HelmCliCommandType.VERSION;
import static io.harness.helm.HelmCommandFlagsUtils.applyHelmCommandFlags;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HelmCommandFlagsUtilsTest extends CategoryTest {
  private static final String COMMAND_WITH_PLACEHOLDER =
      String.format("helm command %s", HelmConstants.HELM_COMMAND_FLAG_PLACEHOLDER);

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyHelmCommandFlags() {
    Map<HelmSubCommandType, String> helmCommands = ImmutableMap.of(HelmSubCommandType.INSTALL, "--namespace default",
        HelmSubCommandType.DELETE, "--no-hooks", HelmSubCommandType.TEMPLATE, "--skip-crds");

    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, INSTALL.name(), helmCommands, HelmVersion.V2))
        .isEqualTo("helm command --namespace default");

    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, DELETE_RELEASE.name(), helmCommands, HelmVersion.V2))
        .isEqualTo("helm command --no-hooks");

    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, RENDER_CHART.name(), helmCommands, HelmVersion.V3))
        .isEqualTo("helm command --skip-crds");

    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, VERSION.name(), helmCommands, HelmVersion.V3))
        .isEqualTo("helm command ");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyHelmCommandsNullOrEmpty() {
    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, INSTALL.name(), null, HelmVersion.V3))
        .isEqualTo("helm command ");
    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, INSTALL.name(), ImmutableMap.of(), HelmVersion.V3))
        .isEqualTo("helm command ");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testApplyHelmCommandsEmptyValueInMap() {
    Map<HelmSubCommandType, String> helmCommands = new HashMap<>();
    helmCommands.put(HelmSubCommandType.INSTALL, null);
    assertThat(applyHelmCommandFlags(COMMAND_WITH_PLACEHOLDER, INSTALL.name(), helmCommands, HelmVersion.V2))
        .isEqualTo("helm command ");
  }
}
