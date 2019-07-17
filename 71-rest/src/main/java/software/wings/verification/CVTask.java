package software.wings.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.ExecutionStatus;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "cvTasks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CVTaskKeys")
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CVTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  @NonNull private String accountId;
  @NonNull private String cvConfigId;
  @NonNull private ExecutionStatus status;

  private long createdAt;
  private long lastUpdatedAt;

  private int retryCount;

  private String exception;
  private int exceptionCode;

  private long startMilliSec;
  private long endMilliSec;
}
