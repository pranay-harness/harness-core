package io.harness.queue;

import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!testEntity")
public class TestInternalEntity implements PersistentEntity {
  @Id private String id;
  private int data;
}
