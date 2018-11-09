package io.harness.registry;

import com.codahale.metrics.Gauge;

/**
 * Created by Pranjal on 11/01/2018
 */
public class CustomGauge implements Gauge<Long> {
  private Long value;

  public CustomGauge(Long value) {
    this.value = value;
  }

  @Override
  public Long getValue() {
    return value;
  }

  public void setValue(Long value) {
    this.value = value;
  }
}
