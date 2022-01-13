/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.GraphNode;
import software.wings.sm.StateExecutionData;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionDetailsInternalMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromGraphNode() {
    assertThat(ExecutionDetailsInternalMetadata.fromGraphNode(null)).isNull();
    ExecutionDetailsInternalMetadata executionDetailsInternalMetadata = ExecutionDetailsInternalMetadata.fromGraphNode(
        GraphNode.builder().executionDetails(prepareExecutionDetailsMap()).build());
    validateExecutionDetailsInternalMetadata(executionDetailsInternalMetadata);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromStateExecutionData() {
    assertThat(ExecutionDetailsInternalMetadata.fromStateExecutionData(null)).isNull();
    StateExecutionData stateExecutionData = mock(StateExecutionData.class);
    when(stateExecutionData.getExecutionDetails()).thenReturn(prepareExecutionDetailsMap());
    ExecutionDetailsInternalMetadata executionDetailsInternalMetadata =
        ExecutionDetailsInternalMetadata.fromStateExecutionData(stateExecutionData);
    validateExecutionDetailsInternalMetadata(executionDetailsInternalMetadata);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromExecutionDetails() {
    assertThat(ExecutionDetailsInternalMetadata.fromExecutionDetails(null)).isNull();
    ExecutionDetailsInternalMetadata executionDetailsInternalMetadata =
        ExecutionDetailsInternalMetadata.fromExecutionDetails(prepareExecutionDetailsMap());
    validateExecutionDetailsInternalMetadata(executionDetailsInternalMetadata);
  }

  private void validateExecutionDetailsInternalMetadata(
      ExecutionDetailsInternalMetadata executionDetailsInternalMetadata) {
    assertThat(executionDetailsInternalMetadata).isNotNull();
    assertThat(executionDetailsInternalMetadata.getInstanceCount()).isNull();
    assertThat(executionDetailsInternalMetadata.getExecutionDetails()).isNotNull();
    assertThat(executionDetailsInternalMetadata.getExecutionDetails().size()).isEqualTo(1);
    assertThat(executionDetailsInternalMetadata.getExecutionDetails().containsKey("k1")).isTrue();
    assertThat(executionDetailsInternalMetadata.getExecutionDetails().get("k1")).isEqualTo("v1");
    assertThat(executionDetailsInternalMetadata.getActivityId()).isEqualTo("aid");
    assertThat(executionDetailsInternalMetadata.getTiming()).isNull();
  }

  private Map<String, ExecutionDataValue> prepareExecutionDetailsMap() {
    return ImmutableMap.of("k1", ExecutionDataValue.builder().value("v1").build(), "activityId",
        ExecutionDataValue.builder().value("aid").build(), "Unit", ExecutionDataValue.builder().value("unit").build());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testConvertExecutionDetailsToMap() {
    assertThat(ExecutionDetailsInternalMetadata.convertExecutionDetailsToMap(null)).isEmpty();
    assertThat(ExecutionDetailsInternalMetadata.convertExecutionDetailsToMap(emptyMap())).isEmpty();
    Map<String, ExecutionDataValue> map = ExecutionDetailsInternalMetadata.convertExecutionDetailsToMap(
        ImmutableMap.of("k1", "v1", "k2", ExecutionDataValue.builder().value("v2").build()));
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.containsKey("k2")).isTrue();
    assertThat(map.get("k2").getValue()).isEqualTo("v2");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanedUpExecutionDetailsMap() {
    assertThat(ExecutionDetailsInternalMetadata.cleanedUpExecutionDetailsMap(null)).isNull();
    Map<String, Object> map = ExecutionDetailsInternalMetadata.cleanedUpExecutionDetailsMap(ImmutableMap.of("Unit",
        ExecutionDataValue.builder().value("v1").build(), "k2", ExecutionDataValue.builder().value("v2").build(), "k3",
        ExecutionDataValue.builder().displayName("dn3").value("v3").build()));
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.containsKey("k2")).isTrue();
    assertThat(map.get("k2")).isEqualTo("v2");
    assertThat(map.containsKey("dn3")).isTrue();
    assertThat(map.get("dn3")).isEqualTo("v3");
  }
}
