package io.harness.steps.resourcerestraint.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Consumer;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "ResourceRestraintInstanceKeys")
@Entity(value = "resourceRestraintInstances")
@Document("resourceRestraintInstances")
public class ResourceRestraintInstance implements PersistentEntity, UuidAccess, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String claimant;

  String resourceRestraintId;
  String resourceUnit;
  int order;

  Consumer.State state;
  int permits;

  String releaseEntityType;
  String releaseEntityId;

  long acquireAt;

  @NonFinal Long nextIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }
}
