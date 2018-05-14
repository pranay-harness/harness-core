package io.harness.time;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class EpochUtilTest {
  @Test
  public void shouldCalculateEpochMilliOfStartOfDayForXDaysInPastFromNow() {
    long forXDaysInPastFromNow =
        EpochUtil.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(30, EpochUtil.PST_ZONE_ID);
    assertThat(forXDaysInPastFromNow).isNotNull();
  }

  @Test
  public void shouldObtainStartOfTheDayEpoch() {
    long startOfTheDayEpoch = EpochUtil.obtainStartOfTheDayEpoch(30, EpochUtil.PST_ZONE_ID);
    assertThat(startOfTheDayEpoch).isNotNull();
  }
}
