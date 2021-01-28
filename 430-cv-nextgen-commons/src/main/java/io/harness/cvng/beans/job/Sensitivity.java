package io.harness.cvng.beans.job;

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
