package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface UpdatedByAccess {
  String LAST_UPDATED_BY_KEY = "lastUpdatedBy";

  EmbeddedUser getLastUpdatedBy();
}
