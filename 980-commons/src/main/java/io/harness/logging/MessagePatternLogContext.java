package io.harness.logging;

public class MessagePatternLogContext extends AutoLogContext {
  public static final String ID = "LogMessagePattern";

  public MessagePatternLogContext(String key, OverrideBehavior behavior) {
    super(ID, key, behavior);
  }
}
