package io.harness.delegate.beans.perpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PerpetualTaskScheduleConfigKeys")
@Entity(value = "perpetualTaskScheduleConfig", noClassnameStored = true)
@OwnedBy(HarnessTeam.PL)
public class PerpetualTaskScheduleConfig implements PersistentEntity {
  private String accountId;
  private String perpetualTaskType;
  private long timeIntervalInMillis;
}
