package io.harness.cvng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
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
  private String verificationJobInstanceId;
  @FdTtlIndex private Date validUntil;
  // TODO: figure out a way to cleanup old/deleted mappings.
}
