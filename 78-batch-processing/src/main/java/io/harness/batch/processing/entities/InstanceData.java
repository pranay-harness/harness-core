package io.harness.batch.processing.entities;

import io.harness.batch.processing.ccm.InstanceResource;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
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

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Entity(value = "instanceData", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_instanceId"), fields = {
    @Field(InstanceDataKeys.accountId), @Field(InstanceDataKeys.instanceId), @Field(InstanceDataKeys.instanceState)
  })
})
@FieldNameConstants(innerTypeName = "InstanceDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstanceData implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String instanceId;
  String appId;
  String serviceId;
  String clusterName;
  String serviceName;
  InstanceResource instanceResource;
  InstanceState instanceState;
  InstanceType instanceType;
  Instant usageStartTime;
  Instant usageStopTime;
  Map<String, String> metaData;

  long createdAt;
  long lastUpdatedAt;
}
