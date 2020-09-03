package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "VerificationTaskKeys")
@Entity(value = "verificationTasks", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class VerificationTask implements UuidAware, CreatedAtAware, AccountAccess, PersistentEntity {
  @Id private String uuid;
  @FdIndex private String accountId;
  private long createdAt;
  private String cvConfigId;
  private String deploymentVerificationTaskId;

  // TODO: figure out a way to cleanup old/deleted mappings.
}
