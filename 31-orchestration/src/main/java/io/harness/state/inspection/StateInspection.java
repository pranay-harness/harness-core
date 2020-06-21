package io.harness.state.inspection;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.state.StateInspectionUtils;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@Entity(value = "stateInspections", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "StateInspectionKeys")
public class StateInspection implements PersistentEntity {
  @Id private String stateExecutionInstanceId;
  private Map<String, StateInspectionData> data;

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plus(StateInspectionUtils.TTL).toInstant());
}
