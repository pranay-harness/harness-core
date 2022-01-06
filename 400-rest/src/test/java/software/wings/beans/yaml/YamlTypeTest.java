/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.appmanifest.ApplicationManifest;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlTypeTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetYamlTypes() {
    List<YamlType> yamlTypes = YamlType.getYamlTypes(ApplicationManifest.class);
    assertThat(yamlTypes).hasSize(18);
    assertThat(yamlTypes).contains(YamlType.APPLICATION_MANIFEST_PCF_ENV_SERVICE_OVERRIDE);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetCompiledPatternForYamlTypePathExpression() {
    String expectedPattern =
        Pattern.compile(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE.getPathExpression()).pattern();

    assertThat(
        YamlType.getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);

    // after caching
    assertThat(
        YamlType.getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_OC_PARAMS_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetCompiledPatternForYamlTypePathExpressionKustomizePatch() {
    String expectedPattern =
        Pattern.compile(YamlType.APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_OVERRIDE.getPathExpression()).pattern();

    assertThat(
        YamlType
            .getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);

    // after caching
    assertThat(
        YamlType
            .getCompiledPatternForYamlTypePathExpression(YamlType.APPLICATION_MANIFEST_KUSTOMIZE_PATCHES_ENV_OVERRIDE)
            .pattern())
        .isEqualTo(expectedPattern);
  }
}
