package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MetricPackDTOTest extends CategoryTest {
  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetMetrics() {
    MetricPackDTO metricPackDTO = MetricPackDTO.builder().build();
    assertThat(metricPackDTO.getMetrics()).isNotNull();
    assertThat(metricPackDTO.getMetrics()).isEmpty();
  }
}
