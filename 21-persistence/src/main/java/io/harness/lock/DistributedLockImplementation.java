package io.harness.lock;

public enum DistributedLockImplementation {
  MONGO("MONGO"),
  REDIS("REDIS");
  private String name;

  DistributedLockImplementation(String name) {
    this.name = name;
  }
}
