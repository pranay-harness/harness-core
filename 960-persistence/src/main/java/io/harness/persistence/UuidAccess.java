package io.harness.persistence;

public interface UuidAccess {
  String UUID_KEY = "uuid";

  String getUuid();

  default String logKeyForId() {
    return LogKeyUtils.logKeyForId(getClass());
  }
}
