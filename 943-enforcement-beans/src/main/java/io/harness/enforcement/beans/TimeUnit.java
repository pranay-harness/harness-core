package io.harness.enforcement.beans;

import java.time.temporal.ChronoUnit;
import lombok.Value;

@Value
public class TimeUnit {
  ChronoUnit unit;
  int numberOfUnits;
}
