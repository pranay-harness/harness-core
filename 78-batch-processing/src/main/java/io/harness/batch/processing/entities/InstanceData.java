package io.harness.batch.processing.entities;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import io.harness.annotation.StoreIn;
import io.harness.batch.processing.ccm.Container;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
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
import java.util.List;
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
@CdIndex(name = "accountId_usageStartTime",
    fields =
    { @Field(InstanceDataKeys.accountId)
      , @Field(value = InstanceDataKeys.usageStartTime, type = IndexType.ASC) })
@FieldNameConstants(innerTypeName = "InstanceDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(EVENTS_DB)
public class InstanceData implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String settingId;
  @FdIndex String instanceId;
  String instanceName;
  String clusterName;
  String clusterId;
  InstanceType instanceType;
  Resource totalResource;
  Resource limitResource;
  Resource allocatableResource;
  List<Container> containerList;
  Map<String, String> labels;
  Map<String, String> metaData;
  Instant usageStartTime;
  Instant usageStopTime;
  InstanceState instanceState;

  long createdAt;
  long lastUpdatedAt;

  HarnessServiceInfo harnessServiceInfo;
}
