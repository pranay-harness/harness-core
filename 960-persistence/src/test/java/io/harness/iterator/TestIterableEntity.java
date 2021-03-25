package io.harness.iterator;

import static io.harness.data.structure.HasPredicate.hasNone;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "!!!testIterable")
@FieldNameConstants(innerTypeName = "TestIterableEntityKeys")
@Slf4j
public class TestIterableEntity implements PersistentRegularIterable {
  @Id private String uuid;
  private String name;
  private String expression;
  private List<Long> nextIterations;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return hasNone(nextIterations) ? null : nextIterations.get(0);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {}
}
