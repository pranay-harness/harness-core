package io.harness.state.inspection;

import io.harness.persistence.PersistentEntity;
import io.harness.state.State;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

@Value
@Builder
@Entity(value = "stateInspections", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "StateInspectionKeys")
public class StateInspection implements PersistentEntity {
  @Id private String stateExecutionInstanceId;
  private Map<String, StateInspectionData> data;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plus(State.TTL).toInstant());
}
