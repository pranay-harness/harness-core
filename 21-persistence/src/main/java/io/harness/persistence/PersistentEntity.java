package io.harness.persistence;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;

import org.mongodb.morphia.annotations.PrePersist;

public class PersistentEntity {
  @PrePersist
  public void onSave() {
    if (this instanceof UuidAware) {
      UuidAware uuidAware = (UuidAware) this;
      if (uuidAware.getUuid() == null) {
        uuidAware.setUuid(generateUuid());
      }
    }

    if (this instanceof CreatedAtAware) {
      CreatedAtAware createdAtAware = (CreatedAtAware) this;
      if (createdAtAware.getCreatedAt() == 0) {
        createdAtAware.setCreatedAt(currentTimeMillis());
      }
    }
  }
}