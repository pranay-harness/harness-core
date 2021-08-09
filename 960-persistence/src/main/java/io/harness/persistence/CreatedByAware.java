package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface CreatedByAware extends CreatedByAccess {
  void setCreatedBy(EmbeddedUser createdBy);
}
