/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics;

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
