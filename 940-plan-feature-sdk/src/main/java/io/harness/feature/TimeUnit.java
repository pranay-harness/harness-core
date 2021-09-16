package io.harness.feature;

import java.time.temporal.ChronoUnit;
import lombok.Value;

@Value
public class TimeUnit {
  ChronoUnit unit;
  int numberOfUnits;
}
