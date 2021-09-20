package software.wings.beans.infrastructure.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@FieldNameConstants(innerTypeName = "InstanceSyncPerpetualTaskStatusKeys")
@Entity(value = "instanceSyncPerpetualTaskStatus", noClassnameStored = true)
@OwnedBy(HarnessTeam.PL)
public class InstanceSyncPerpetualTaskStatus implements PersistentEntity {
  String perpetualTaskId;
  String lastFailureReason;
  long initialFailureAt;
}
