package io.harness.batch.processing.entities;

import io.harness.batch.processing.entities.ActiveInstance.ActiveInstanceKeys;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Data
@Builder
@Entity(value = "activeInstance", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_instanceId_createdAt"),
      fields =
      {
        @Field(ActiveInstanceKeys.accountId)
        , @Field(ActiveInstanceKeys.instanceId), @Field(ActiveInstanceKeys.createdAt)
      })
  ,
      @Index(options = @IndexOptions(name = "accountId_createdAt"), fields = {
        @Field(ActiveInstanceKeys.accountId), @Field(ActiveInstanceKeys.createdAt)
      })
})
@FieldNameConstants(innerTypeName = "ActiveInstanceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveInstance implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String clusterId;
  String instanceId;
  String typeUrl;
  long createdAt;
  long lastUpdatedAt;
}
