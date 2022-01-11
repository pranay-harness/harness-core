/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.expression;

import static io.harness.rule.OwnerRule.HINGER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ShellScriptEnvironmentVariablesTest extends WingsBaseTest {
  Map<String, String> outputVars;
  Map<String, String> secretOutputVars;

  ShellScriptEnvironmentVariables shellScriptEnvironmentVariables;

  @Before
  public void setUp() {
    outputVars = ImmutableMap.of("var1", "val1");
    secretOutputVars = ImmutableMap.of("secretVar", "secretVal");
    shellScriptEnvironmentVariables = new ShellScriptEnvironmentVariables(outputVars, secretOutputVars);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testGetForStringSecretVariable() {
    assertThat(shellScriptEnvironmentVariables.get("var1")).isEqualTo("val1");
    assertThat(shellScriptEnvironmentVariables.get("secretVar"))
        .isEqualTo("${sweepingOutputSecrets.obtain(\""
            + "secretVar"
            + "\",\""
            + "secretVal"
            + "\")}");
  }
}
