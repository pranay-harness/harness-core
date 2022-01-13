/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.formatter;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.export.metadata.WorkflowExecutionMetadata;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JsonFormatterTest extends CategoryTest {
  private static final String OUTPUT_FORMAT = "{\"key\":\"%s\"}";

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetOutputString() {
    JsonFormatter jsonFormatter = new JsonFormatter();
    String key = "abc";
    String output = jsonFormatter.getOutputString(new SampleClass(key));
    assertThat(output).isNotNull();
    assertThat(output.replaceAll("\\s", "")).isEqualTo(format(OUTPUT_FORMAT, key));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetOutputBytes() {
    JsonFormatter jsonFormatter = new JsonFormatter();
    String key = "abc";
    byte[] outputBytes = jsonFormatter.getOutputBytes(new SampleClass(key));
    assertThat(outputBytes).isNotNull();
    assertThat(new String(outputBytes).replaceAll("\\s", "")).isEqualTo(format(OUTPUT_FORMAT, key));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetExecutionMetadataOutputBytes() {
    JsonFormatter jsonFormatter = new JsonFormatter();
    byte[] outputBytes =
        jsonFormatter.getExecutionMetadataOutputBytes(WorkflowExecutionMetadata.builder().id("id").build());
    assertThat(outputBytes).isNotNull();
    assertThat(new String(outputBytes).replaceAll("\\s", "")).isEqualTo("{\"onDemandRollback\":false}");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetExtension() {
    JsonFormatter jsonFormatter = new JsonFormatter();
    assertThat(jsonFormatter.getExtension()).isEqualTo("json");
  }

  private static class SampleClass {
    public String key;
    public String key2;

    SampleClass(String key) {
      this.key = key;
      this.key2 = null;
    }
  }
}
