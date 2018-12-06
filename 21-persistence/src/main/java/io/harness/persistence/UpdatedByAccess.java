package io.harness.persistence;

public interface UpdatedByAccess {
  String LAST_UPDATED_BY_KEY = "lastUpdatedBy";

  long getLastUpdatedBy();
}
