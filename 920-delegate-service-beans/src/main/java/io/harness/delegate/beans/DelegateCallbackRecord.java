package io.harness.delegate.beans;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdTtlIndex;
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
@Entity(value = "delegateCallbacks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateCallbackRecordKeys")
@StoreIn("dms")
public class DelegateCallbackRecord implements PersistentEntity {
  @Id private String uuid;
  private byte[] callbackMetadata;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
