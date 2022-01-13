/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.steps.shellscript.ShellScriptInlineSource;
import io.harness.steps.shellscript.ShellScriptStepParameters;
import io.harness.steps.shellscript.ShellType;
import io.harness.utils.YamlPipelineUtils;

import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class PipelineYamlTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testShellScriptStepSerialization() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/shellScriptStep.yml");
    ShellScriptStepParameters shellScriptStepParameters =
        YamlPipelineUtils.read(testFile, ShellScriptStepParameters.class);
    assertThat(shellScriptStepParameters.getOnDelegate().getValue()).isEqualTo(true);
    assertThat(shellScriptStepParameters.getShell()).isEqualTo(ShellType.Bash);
    assertThat(shellScriptStepParameters.getSource().getType()).isEqualTo("Inline");
    assertThat(((ShellScriptInlineSource) shellScriptStepParameters.getSource().getSpec()).getScript().getValue())
        .isEqualTo("echo hi");
  }
}
