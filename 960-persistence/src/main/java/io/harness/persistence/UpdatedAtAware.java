package io.harness.persistence;

public interface UpdatedAtAware extends UpdatedAtAccess {
  String LAST_UPDATED_AT_KEY = "lastUpdatedAt";

  void setLastUpdatedAt(long lastUpdatedAt);
}
