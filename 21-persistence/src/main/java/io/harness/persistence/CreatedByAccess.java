package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface CreatedByAccess {
  String CREATED_BY_KEY = "createdBy";

  EmbeddedUser getCreatedBy();
}
