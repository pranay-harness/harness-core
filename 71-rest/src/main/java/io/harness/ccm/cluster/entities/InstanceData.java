package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.InstanceData.InstanceDataKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
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
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.instance.HarnessServiceInfo;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@Entity(value = "instanceData", noClassnameStored = true)

@CdIndex(name = "accountId_clusterId_instanceId_instanceState",
    fields =
    {
      @Field(InstanceDataKeys.accountId)
      , @Field(InstanceDataKeys.clusterId), @Field(InstanceDataKeys.instanceId), @Field(InstanceDataKeys.instanceState)
    })
@CdIndex(name = "accountId_clusterId_instanceName_usageStartTime",
    fields =
    {
      @Field(InstanceDataKeys.accountId)
      , @Field(InstanceDataKeys.clusterId), @Field(InstanceDataKeys.instanceName),
          @Field(value = InstanceDataKeys.usageStartTime, type = IndexType.DESC)
    })
@CdIndex(name = "accountId_clusterId_instanceState_usageStartTime",
    fields =
    {
      @Field(InstanceDataKeys.accountId)
      , @Field(InstanceDataKeys.clusterId), @Field(InstanceDataKeys.instanceState),
          @Field(value = InstanceDataKeys.usageStartTime)
    })
@CdIndex(name = "accountId_usageStartTime_usageStopTime",
    fields =
    {
      @Field(InstanceDataKeys.accountId)
      , @Field(value = InstanceDataKeys.usageStartTime), @Field(value = InstanceDataKeys.usageStopTime)
    })

@FieldNameConstants(innerTypeName = "InstanceDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
public class InstanceData implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String settingId;
  @FdIndex String instanceId;
  String instanceName;
  String clusterName;
  String clusterId;
  Resource totalResource;
  Resource allocatableResource;
  Map<String, String> labels;
  Map<String, String> metaData;
  Instant usageStartTime;
  Instant usageStopTime;
  String instanceState;

  long createdAt;
  long lastUpdatedAt;

  HarnessServiceInfo harnessServiceInfo;
}
