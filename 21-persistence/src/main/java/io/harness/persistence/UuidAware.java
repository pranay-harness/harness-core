package io.harness.persistence;

public interface UuidAware {
  String ID_KEY = "_id";

  String getUuid();
  void setUuid(String uuid);
}
