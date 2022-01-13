/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionHistoryMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromStateExecutionDataList() {
    assertThat(ExecutionHistoryMetadata.fromStateExecutionDataList(null)).isNull();
    StateExecutionData stateExecutionData1 = mock(StateExecutionData.class);
    when(stateExecutionData1.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    when(stateExecutionData1.getExecutionDetails()).thenReturn(prepareExecutionDetailsMap());
    StateExecutionData stateExecutionData2 = mock(StateExecutionData.class);
    when(stateExecutionData2.getStateName()).thenReturn("sn");
    when(stateExecutionData2.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    when(stateExecutionData2.getExecutionDetails()).thenReturn(prepareExecutionDetailsMap());
    List<ExecutionHistoryMetadata> executionHistoryMetadataList =
        ExecutionHistoryMetadata.fromStateExecutionDataList(asList(stateExecutionData1, null, stateExecutionData2));
    assertThat(executionHistoryMetadataList).isNotNull();
    assertThat(executionHistoryMetadataList.size()).isEqualTo(2);
    validateExecutionHistoryMetadata(executionHistoryMetadataList.get(0));
    validateExecutionHistoryMetadata(executionHistoryMetadataList.get(1), 2);
    assertThat(executionHistoryMetadataList.get(1).getName()).isEqualTo("sn_3");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromStateExecutionData() {
    assertThat(ExecutionHistoryMetadata.fromStateExecutionData(0, null)).isNull();
    StateExecutionData stateExecutionData = mock(StateExecutionData.class);
    when(stateExecutionData.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    when(stateExecutionData.getExecutionDetails()).thenReturn(prepareExecutionDetailsMap());
    ExecutionHistoryMetadata executionHistoryMetadata =
        ExecutionHistoryMetadata.fromStateExecutionData(0, stateExecutionData);
    validateExecutionHistoryMetadata(executionHistoryMetadata);
  }

  private void validateExecutionHistoryMetadata(ExecutionHistoryMetadata executionHistoryMetadata) {
    validateExecutionHistoryMetadata(executionHistoryMetadata, 0);
  }

  private void validateExecutionHistoryMetadata(ExecutionHistoryMetadata executionHistoryMetadata, int idx) {
    assertThat(executionHistoryMetadata).isNotNull();
    assertThat(executionHistoryMetadata.getName()).endsWith("_" + (idx + 1));
    assertThat(executionHistoryMetadata.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionHistoryMetadata.getExecutionDetails()).isNotNull();
    assertThat(executionHistoryMetadata.getExecutionDetails().size()).isEqualTo(1);
    assertThat(executionHistoryMetadata.getExecutionDetails().containsKey("k1")).isTrue();
    assertThat(executionHistoryMetadata.getExecutionDetails().get("k1")).isEqualTo("v1");
    assertThat(executionHistoryMetadata.getActivityId()).isEqualTo("aid");
    assertThat(executionHistoryMetadata.getTiming()).isNull();
  }

  private Map<String, ExecutionDataValue> prepareExecutionDetailsMap() {
    return ImmutableMap.of("k1", ExecutionDataValue.builder().value("v1").build(), "activityId",
        ExecutionDataValue.builder().value("aid").build(), "Unit", ExecutionDataValue.builder().value("unit").build());
  }
}
