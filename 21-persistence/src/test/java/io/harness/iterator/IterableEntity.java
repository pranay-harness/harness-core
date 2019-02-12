package io.harness.iterator;

import io.harness.persistence.PersistentIterable;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "!!!testIterable")
public class IterableEntity implements PersistentIterable {
  public static final String NEXT_ITERATION_KEY = "nextIteration";

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
