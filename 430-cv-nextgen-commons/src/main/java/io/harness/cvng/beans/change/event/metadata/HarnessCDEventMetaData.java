package io.harness.cvng.beans.change.event.metadata;

import io.harness.cvng.beans.change.event.type.ChangeSourceType;
import io.harness.pms.contracts.execution.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HarnessCDEventMetaData extends ChangeEventMetaData {
  long deploymentStartTime;
  long deploymentEndTime;
  String executionId;
  String stageId;
  Status status;

  @Override
  public ChangeSourceType getType() {
    return ChangeSourceType.HARNESS_CD;
  }
}
