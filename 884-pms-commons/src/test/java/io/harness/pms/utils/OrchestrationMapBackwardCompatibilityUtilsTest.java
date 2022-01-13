/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.data.OrchestrationMap;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationMapBackwardCompatibilityUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testExtractToOrchestrationMap() {
    assertThat(OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(null)).isEmpty();

    Document doc = new Document();
    doc.put("k11", "v11");
    doc.put("k12", "v12");
    OrchestrationMap orchestrationMap = OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(doc);
    assertThat(orchestrationMap).isNotNull();
    assertThat(orchestrationMap.size()).isEqualTo(2);
    assertThat(orchestrationMap.get("k11")).isEqualTo("v11");
    assertThat(orchestrationMap.get("k12")).isEqualTo("v12");

    orchestrationMap = OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(
        ImmutableMap.of("k21", "v21", "k22", "v22"));
    assertThat(orchestrationMap).isNotNull();
    assertThat(orchestrationMap.size()).isEqualTo(2);
    assertThat(orchestrationMap.get("k21")).isEqualTo("v21");
    assertThat(orchestrationMap.get("k22")).isEqualTo("v22");

    assertThatThrownBy(() -> OrchestrationMapBackwardCompatibilityUtils.extractToOrchestrationMap(10L))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testConvertToOrchestrationMap() {
    assertThat(OrchestrationMapBackwardCompatibilityUtils.convertToOrchestrationMap(null)).isNull();

    Map<String, OrchestrationMap> m = OrchestrationMapBackwardCompatibilityUtils.convertToOrchestrationMap(
        ImmutableMap.of("k", ImmutableMap.of("k1", "v1")));
    assertThat(m).isNotNull();
    assertThat(m.get("k")).isNotNull();

    OrchestrationMap orchestrationMap = m.get("k");
    assertThat(orchestrationMap).isNotNull();
    assertThat(orchestrationMap.size()).isEqualTo(1);
    assertThat(orchestrationMap.get("k1")).isEqualTo("v1");
  }
}
