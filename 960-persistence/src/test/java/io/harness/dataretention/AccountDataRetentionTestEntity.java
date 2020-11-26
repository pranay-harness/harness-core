package io.harness.dataretention;

import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;

import java.util.Date;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!accountDataRetentionTestEntities", noClassnameStored = true)
public class AccountDataRetentionTestEntity implements PersistentEntity, AccountDataRetentionEntity {
  @Id private String uuid;
  private String accountId;
  private long createdAt;

  @FdTtlIndex private Date validUntil;
}
