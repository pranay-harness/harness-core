package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.ExecutionDataValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceCountMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testExtractFromExecutionDetails() {
    assertThat(InstanceCountMetadata.extractFromExecutionDetails(null)).isNull();
    assertThat(InstanceCountMetadata.extractFromExecutionDetails(Collections.emptyMap())).isNull();

    Map<String, ExecutionDataValue> executionDetailsMap = new HashMap<>();
    executionDetailsMap.put("Succeeded", ExecutionDataValue.builder().value(1).build());
    assertThat(InstanceCountMetadata.extractFromExecutionDetails(executionDetailsMap)).isNull();

    executionDetailsMap.put("Total instances", ExecutionDataValue.builder().value(-1).build());
    assertThat(InstanceCountMetadata.extractFromExecutionDetails(executionDetailsMap)).isNull();

    executionDetailsMap.put("Total instances", ExecutionDataValue.builder().value(2).build());
    InstanceCountMetadata instanceCountMetadata =
        InstanceCountMetadata.extractFromExecutionDetails(executionDetailsMap);
    assertThat(instanceCountMetadata).isNotNull();
    assertThat(instanceCountMetadata.getTotal()).isEqualTo(2);
    assertThat(instanceCountMetadata.getSucceeded()).isEqualTo(1);
    assertThat(instanceCountMetadata.getFailed()).isNull();
  }
}
