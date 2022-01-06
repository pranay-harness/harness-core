/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
