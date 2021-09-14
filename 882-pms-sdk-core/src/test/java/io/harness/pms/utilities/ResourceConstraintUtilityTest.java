/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.utilities;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResourceConstraintUtilityTest extends PmsSdkCoreTestBase {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetResourceConstraintJsonNode() {
    String resourceConstraint = "test";
    JsonNode jsonNode = ResourceConstraintUtility.getResourceConstraintJsonNode(resourceConstraint);
    assertThat(JsonNodeUtils.getString(jsonNode, "name")).isEqualTo("Resource Constraint");
    assertThat(JsonNodeUtils.getString(jsonNode, "timeout")).isEqualTo("1w");
    assertThat(JsonNodeUtils.getString(jsonNode, "type")).isEqualTo("ResourceConstraint");
    assertThat(JsonNodeUtils.getMap(jsonNode, "spec").get("name").asText()).isEqualTo("Queuing");
    assertThat(JsonNodeUtils.getMap(jsonNode, "spec").get("resourceUnit").asText()).isEqualTo(resourceConstraint);
  }
}
