package io.harness.persistence;

public interface UuidAccess {
  String getUuid();

  default String logKeyForId() {
    return LogKeyUtils.logKeyForId(getClass());
  }
}
