package io.harness.iterator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Entity(value = "!!!testCronIterable")
@FieldNameConstants(innerTypeName = "CronIterableEntityKeys")
@Slf4j
public class CronIterableEntity implements PersistentCronIterable {
  @Id private String uuid;
  private String name;
  private String expression;
  private List<Long> nextIterations;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return isEmpty(nextIterations) ? null : nextIterations.get(0);
  }

  @Override
  public List<Long> recalculateNextIterations(String fieldName) {
    if (nextIterations == null) {
      nextIterations = new ArrayList<>();
    }

    if (expandNextIterations(expression, nextIterations)) {
      logger.info(expression);
      return nextIterations;
    }

    return null;
  }

  @Override
  public boolean skipMissed() {
    return true;
  }
}
