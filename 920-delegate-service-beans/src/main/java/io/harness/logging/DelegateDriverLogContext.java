package io.harness.logging;

public class DelegateDriverLogContext extends AutoLogContext {
  public static final String ID = "driverId";

  public DelegateDriverLogContext(String driverId, OverrideBehavior behavior) {
    super(ID, driverId, behavior);
  }
}
