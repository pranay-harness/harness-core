package io.harness.logging;

public class MdcKeyLogContext extends AutoLogContext {
  public static final String ID = "MDCKey";

  public MdcKeyLogContext(String key, OverrideBehavior behavior) {
    super(ID, key, behavior);
  }
}
