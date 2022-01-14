package io.harness.cvng;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class CVNGTestConstants {
  public static Instant TIME_FOR_TESTS = Instant.parse("2020-07-27T10:50:00Z");
  public static Clock FIXED_TIME_FOR_TESTS = Clock.fixed(TIME_FOR_TESTS, ZoneOffset.UTC);
}
