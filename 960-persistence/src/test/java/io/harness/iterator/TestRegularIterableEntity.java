package io.harness.iterator;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "!!!testRegularIterable")
@FieldNameConstants(innerTypeName = "RegularIterableEntityKeys")
public class TestRegularIterableEntity implements PersistentRegularIterable {
  @Id private String uuid;
  private Long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }
}
