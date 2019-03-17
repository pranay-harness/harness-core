package io.harness.time;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EpochUtilTest {
  @Test
  @Category(UnitTests.class)
  public void shouldCalculateEpochMilliOfStartOfDayForXDaysInPastFromNow() {
    long forXDaysInPastFromNow =
        EpochUtils.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(30, EpochUtils.PST_ZONE_ID);
    assertThat(forXDaysInPastFromNow).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldObtainStartOfTheDayEpoch() {
    long startOfTheDayEpoch = EpochUtils.obtainStartOfTheDayEpoch(30, EpochUtils.PST_ZONE_ID);
    assertThat(startOfTheDayEpoch).isNotNull();
  }
}
