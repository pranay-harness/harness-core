package io.harness.persistence;

public interface UpdatedAtAccess {
  String LAST_UPDATED_AT_KEY = "lastUpdatedAt";

  long getLastUpdatedAt();
}
