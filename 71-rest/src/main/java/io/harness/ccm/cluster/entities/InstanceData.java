package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.InstanceData.InstanceDataKeys;
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
import software.wings.beans.instance.HarnessServiceInfo;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Entity(value = "instanceData", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_clusterId_instanceId"), fields = {
    @Field(InstanceDataKeys.accountId), @Field(InstanceDataKeys.clusterId), @Field(InstanceDataKeys.instanceId)
  })
})
@FieldNameConstants(innerTypeName = "InstanceDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
public class InstanceData implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String settingId;
  String instanceId;
  String instanceName;
  String clusterName;
  String clusterId;
  Resource totalResource;
  Resource allocatableResource;
  Map<String, String> labels;
  Map<String, String> metaData;
  Instant usageStartTime;
  Instant usageStopTime;

  long createdAt;
  long lastUpdatedAt;

  HarnessServiceInfo harnessServiceInfo;
}
