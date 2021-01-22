package io.harness.cvng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Date;
import java.util.List;
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
  public static final String VERIFICATION_TASK_ID_KEY = "verificationTaskId";

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("query_idx")
                 .field(VerificationTaskKeys.verificationJobInstanceId)
                 .field(VerificationTaskKeys.accountId)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String accountId;
  @FdIndex private long createdAt;
  @FdIndex private String cvConfigId;
  private String verificationJobInstanceId;
  @FdTtlIndex private Date validUntil;
  // TODO: figure out a way to cleanup old/deleted mappings.
}
