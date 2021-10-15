package io.harness.delegate.beans;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!custom_delegateSyncTaskResponses", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateSyncTaskResponseKeys")
@StoreIn("dms")
public class DelegateSyncTaskResponse implements PersistentEntity {
  @Id @org.springframework.data.annotation.Id private String uuid;
  private byte[] responseData;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusHours(2).toInstant());
}
