package io.harness.ccm.commons.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.commons.beans.Container;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.beans.StorageResource;
import io.harness.ccm.commons.entities.InstanceData.InstanceDataKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

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
  String cloudProviderInstanceId;
  InstanceType instanceType;
  Resource totalResource;
  Resource limitResource;
  Resource allocatableResource;
  StorageResource storageResource;
  List<Container> containerList;
  Map<String, String> labels;
  Map<String, String> namespaceLabels;
  Map<String, String> metaData;
  Instant usageStartTime;
  Instant usageStopTime;
  InstanceState instanceState;

  long createdAt;
  long lastUpdatedAt;

  HarnessServiceInfo harnessServiceInfo;

  public static final class InstanceDataKeys {
    private InstanceDataKeys() {}
    public static final String CLOUD_PROVIDER = "metaData.cloud_provider";
  }
}
