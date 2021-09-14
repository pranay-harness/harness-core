/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature;

import java.time.temporal.ChronoUnit;
import lombok.Value;

@Value
public class TimeUnit {
  ChronoUnit unit;
  int numberOfUnits;
}
