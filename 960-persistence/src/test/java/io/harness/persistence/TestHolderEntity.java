package io.harness.persistence;

import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!testHolder", noClassnameStored = true)
public class TestHolderEntity implements PersistentEntity {
  @Id private String uuid;
  MorphiaInterface morphiaObj;
}
