package io.harness.iterator;

import io.harness.persistence.PersistentIterable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "!!!testIterable")
@FieldNameConstants(innerTypeName = "IterableEntityKeys")
public class IterableEntity implements PersistentIterable {
  @Id private String uuid;
  private Long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }
}
