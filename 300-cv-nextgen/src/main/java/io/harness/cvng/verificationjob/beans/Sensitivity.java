package io.harness.cvng.verificationjob.beans;

public enum Sensitivity {
  LOW(1),
  MEDIUM(2),
  HIGH(3);
  private final int tolerance;

  Sensitivity(int tolerance) {
    this.tolerance = tolerance;
  }

  public int getTolerance() {
    return tolerance;
  }
}
