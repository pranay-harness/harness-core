package io.harness.feature.beans;

import java.time.temporal.ChronoUnit;
import lombok.Value;

@Value
public class TimeUnit {
  ChronoUnit unit;
  int numberOfUnits;
}
