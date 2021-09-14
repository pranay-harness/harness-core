/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.appdynamics;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 5/17/17.
 */
@Data
@Builder
public class AppdynamicsMetricDataValue {
  private long startTimeInMillis;
  private double value;
  private long min;
  private long max;
  private long current;
  private long sum;
  private long count;
  private double standardDeviation;
  private int occurrences;
  private boolean useRange;
}
