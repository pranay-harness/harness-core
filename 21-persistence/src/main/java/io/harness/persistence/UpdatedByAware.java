package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface UpdatedByAware extends UpdatedByAccess {
  void setLastUpdatedBy(EmbeddedUser lastUpdatedBy);
}
