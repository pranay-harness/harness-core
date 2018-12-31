package io.harness.queue;

import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!testEntity")
public class TestInternalEntity {
  @Id private String id;
  private int data;
}
