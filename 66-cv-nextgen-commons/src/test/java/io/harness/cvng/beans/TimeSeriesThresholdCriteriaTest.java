package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeSeriesThresholdCriteriaTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSetCriteria_whenNoStartSymbol() {
    assertThatThrownBy(() -> TimeSeriesThresholdCriteria.builder().criteria("sdlsdnsd").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("criteria has to start with '> ' or '< '");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSetCriteria_whenNoInvalidLength() {
    assertThatThrownBy(() -> TimeSeriesThresholdCriteria.builder().criteria("> 10. 0").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("the criteria has to be defined like '> 5.0' or '< 5.0");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSetCriteria_whenGreaterThan() {
    final TimeSeriesThresholdCriteria timeSeriesThresholdCriteria =
        TimeSeriesThresholdCriteria.builder().criteria("  >   10.0  ").build();
    assertThat(timeSeriesThresholdCriteria.getValue()).isEqualTo(10.0, offset(0.01));
    assertThat(timeSeriesThresholdCriteria.getThresholdType()).isEqualTo(TimeSeriesThresholdType.ACT_WHEN_HIGHER);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSetCriteria_whenLessThan() {
    final TimeSeriesThresholdCriteria timeSeriesThresholdCriteria =
        TimeSeriesThresholdCriteria.builder().criteria("  <   10.0  ").build();
    assertThat(timeSeriesThresholdCriteria.getValue()).isEqualTo(10.0, offset(0.01));
    assertThat(timeSeriesThresholdCriteria.getThresholdType()).isEqualTo(TimeSeriesThresholdType.ACT_WHEN_LOWER);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSetCriteria_whenNoSpace() {
    final TimeSeriesThresholdCriteria timeSeriesThresholdCriteria =
        TimeSeriesThresholdCriteria.builder().criteria("    <10.0  ").build();
    assertThat(timeSeriesThresholdCriteria.getValue()).isEqualTo(10.0, offset(0.01));
    assertThat(timeSeriesThresholdCriteria.getThresholdType()).isEqualTo(TimeSeriesThresholdType.ACT_WHEN_LOWER);
  }
}