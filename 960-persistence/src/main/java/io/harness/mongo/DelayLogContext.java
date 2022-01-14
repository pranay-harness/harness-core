package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class DelayLogContext extends AutoLogContext {
  public static final String ID = "delay";

  public DelayLogContext(Long delay, OverrideBehavior behavior) {
    super(ID, delay.toString(), behavior);
  }
}
