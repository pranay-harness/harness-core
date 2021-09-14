/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.exception;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class YamlNodeErrorInfoTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testFromField() throws IOException {
    String yaml = "step:\n"
        + "  identifier: search\n"
        + "  name: search\n"
        + "  type: Http\n"
        + "  spec:\n"
        + "    url: https://www.google.com";
    YamlField yamlField = YamlUtils.readTree(yaml);
    YamlField yamlField1 = yamlField.getNode().getField("step");
    assertThat(yamlField1).isNotNull();
    YamlNodeErrorInfo yamlNodeErrorInfo = YamlNodeErrorInfo.fromField(yamlField1);
    assertThat(yamlNodeErrorInfo).isNotNull();
    assertThat(yamlNodeErrorInfo.getIdentifier()).isEqualTo("search");
    assertThat(yamlNodeErrorInfo.getName()).isEqualTo("step");
    assertThat(yamlNodeErrorInfo.getType()).isEqualTo("Http");
  }
}
