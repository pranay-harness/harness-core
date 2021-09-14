/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
