/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.yaml;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class YamlNodeTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetLastKeyInPath() {
    assertThat(YamlNode.getLastKeyInPath("pipeline/stages/[1]/stage/spec/execution/steps/[1]/step/spec/connectorRef"))
        .isEqualTo("connectorRef");
    assertThat(YamlNode.getLastKeyInPath("pipeline/stages/[1]")).isEqualTo("[1]");
    assertThat(YamlNode.getLastKeyInPath("pipeline/stages/[1]/stage/spec/serviceConfig/serviceRef"))
        .isEqualTo("serviceRef");
    assertThat(YamlNode.getLastKeyInPath("pipeline")).isEqualTo("pipeline");
    assertThatThrownBy(() -> YamlNode.getLastKeyInPath("")).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> YamlNode.getLastKeyInPath(null)).isInstanceOf(InvalidRequestException.class);
  }
}
