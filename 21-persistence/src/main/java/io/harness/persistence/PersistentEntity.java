package io.harness.persistence;

import static java.lang.System.currentTimeMillis;

import org.mongodb.morphia.annotations.PrePersist;

public class PersistentEntity {
  @PrePersist
  public void onSave() {
    if (this instanceof CreatedAtAware) {
      CreatedAtAware createdAtAware = (CreatedAtAware) this;
      if (createdAtAware.getCreatedAt() == 0) {
        createdAtAware.setCreatedAt(currentTimeMillis());
      }
    }
  }
}
