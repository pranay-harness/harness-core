/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.formatter;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.export.request.ExportExecutionsRequest.OutputFormat;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OutputFormatterTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetOutputString() {
    assertThatThrownBy(() -> OutputFormatter.fromOutputFormat(null)).isInstanceOf(NullPointerException.class);

    OutputFormatter outputFormatter = OutputFormatter.fromOutputFormat(OutputFormat.JSON);
    assertThat(outputFormatter).isNotNull();
    assertThat(outputFormatter).isInstanceOf(JsonFormatter.class);
  }
}
