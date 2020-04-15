package io.harness.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.ExecutionStatus;
import io.harness.persistence.AccountAccess;
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
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PrePersist;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.DataCollectionInfoV2;

import java.time.OffsetDateTime;
import java.util.Date;

@FieldNameConstants(innerTypeName = "CVTaskKeys")
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "cvTasks", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class CVTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;

  @NonNull @Indexed private String accountId;
  private String cvConfigId;
  @Indexed private String stateExecutionId;
  private String nextTaskId;
  @Indexed @NonNull private ExecutionStatus status;

  private long createdAt;
  @Indexed private long lastUpdatedAt;

  private int retryCount;

  private String exception;
  private long validAfter;
  private String correlationId;
  private DataCollectionInfoV2 dataCollectionInfo;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
  @PrePersist
  public void onUpdate() {
    // better to add days as plus month can vary and add complications to testing etc.
    validUntil = Date.from(OffsetDateTime.now().plusDays(VerificationConstants.CV_TASK_TTL_MONTHS * 30).toInstant());
  }
}
