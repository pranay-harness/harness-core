package io.harness.adviser;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class AdviserType {
  // Provided From the orchestration layer system advisers

  // SUCCESS
  public static final String ON_SUCCESS = "ON_SUCCESS";

  // FAILURES
  public static final String ON_FAIL = "ON_FAIL";
  public static final String IGNORE = "IGNORE";
  public static final String RETRY = "RETRY";

  // Interrupts
  public static final String ABORT = "ABORT";
  public static final String PAUSE = "PAUSE";
  public static final String RESUME = "RESUME";

  String type;
}
