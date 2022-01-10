/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static io.harness.rule.OwnerRule.PUNEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.Owner;

import software.wings.utils.PowerShellScriptsLoader.PsScript;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PowerShellScriptsLoaderTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    for (Map.Entry<PsScript, String> entry : PowerShellScriptsLoader.psScriptMap.entrySet()) {
      assertThat(EmptyPredicate.isEmpty(entry.getValue())).isFalse();
    }
  }
}
