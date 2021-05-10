package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData;
import static io.harness.timescaledb.Tables.NODE_INFO;
import static io.harness.timescaledb.Tables.POD_INFO;
import static io.harness.timescaledb.Tables.WORKLOAD_INFO;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import io.harness.annotations.retry.RetryOnException;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.service.intfc.InstanceInfoTimescaleDAO;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.beans.Resource;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.timescaledb.Keys;
import io.harness.timescaledb.tables.records.NodeInfoRecord;
import io.harness.timescaledb.tables.records.PodInfoRecord;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.InsertFinalStep;
import org.jooq.InsertSetStep;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdateFinalStep;
import org.jooq.UpdateSetMoreStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceInfoTimescaleDAOImpl implements InstanceInfoTimescaleDAO {
  private final DSLContext dslContext;

  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;
  private static final ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;

  @Autowired
  public InstanceInfoTimescaleDAOImpl(DSLContext dslContext) {
    this.dslContext = dslContext;
  }

  @Override
  public void insertIntoNodeInfo(@NotNull InstanceInfo instanceInfo) {
    final String nodePoolName =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NODE_POOL_NAME, instanceInfo.getMetaData());

    insertOne(dslContext.insertInto(NODE_INFO)
                  .set(NODE_INFO.ACCOUNTID, instanceInfo.getAccountId())
                  .set(NODE_INFO.CLUSTERID, instanceInfo.getClusterId())
                  .set(NODE_INFO.INSTANCEID, instanceInfo.getInstanceId())
                  .set(NODE_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
                  .set(NODE_INFO.NODEPOOLNAME, nodePoolName)
                  .onConflictOnConstraint(Keys.NODE_INFO_UNIQUE_RECORD_INDEX)
                  .doUpdate()
                  .set(NODE_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
                  .set(NODE_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                  .set(NODE_INFO.NODEPOOLNAME, nodePoolName));
  }

  @Override
  public void insertIntoNodeInfo(@NotNull List<InstanceInfo> instanceInfoList) {
    instanceInfoList.forEach(this::insertIntoNodeInfo);
  }

  @Override
  public void insertIntoWorkloadInfo(@NotNull String accountId, @NotNull K8sWorkloadSpec workloadSpec) {
    int replicas = Integer.parseInt(String.valueOf(workloadSpec.getReplicas()));

    insertOne(dslContext.insertInto(WORKLOAD_INFO)
                  .set(WORKLOAD_INFO.ACCOUNTID, accountId)
                  .set(WORKLOAD_INFO.CLUSTERID, workloadSpec.getClusterId())
                  .set(WORKLOAD_INFO.WORKLOADID, workloadSpec.getUid())
                  .set(WORKLOAD_INFO.REPLICAS, replicas)
                  .set(WORKLOAD_INFO.TYPE, workloadSpec.getWorkloadKind())
                  .set(WORKLOAD_INFO.NAME, workloadSpec.getWorkloadName())
                  .set(WORKLOAD_INFO.NAMESPACE, workloadSpec.getNamespace())
                  .onConflictOnConstraint(Keys.WORKLOAD_INFO_UNIQUE_RECORD_INDEX)
                  .doUpdate()
                  .set(WORKLOAD_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                  .set(WORKLOAD_INFO.REPLICAS, replicas)
                  .set(WORKLOAD_INFO.TYPE, workloadSpec.getWorkloadKind())
                  .set(WORKLOAD_INFO.NAME, workloadSpec.getWorkloadName())
                  .set(WORKLOAD_INFO.NAMESPACE, workloadSpec.getNamespace()));
  }

  @Override
  public void insertIntoPodInfo(@NotNull List<InstanceInfo> instanceInfoList) {
    instanceInfoList.forEach(this::insertIntoPodInfo);
  }

  @Override
  public void insertIntoPodInfo(@NotNull InstanceInfo instanceInfo) {
    final String workloadId =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_ID, instanceInfo.getMetaData());
    final String namespace =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NAMESPACE, instanceInfo.getMetaData());
    final String parentId = getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceInfo.getMetaData());
    final Resource resource = firstNonNull(instanceInfo.getResource(), Resource.builder().build());

    insertOne(dslContext.insertInto(POD_INFO)
                  .set(POD_INFO.ACCOUNTID, instanceInfo.getAccountId())
                  .set(POD_INFO.CLUSTERID, instanceInfo.getClusterId())
                  .set(POD_INFO.INSTANCEID, instanceInfo.getInstanceId())
                  .set(POD_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
                  .set(POD_INFO.NAMESPACE, namespace)
                  .set(POD_INFO.NAME, instanceInfo.getInstanceName())
                  .set(POD_INFO.WORKLOADID, workloadId)
                  .set(POD_INFO.CPUREQUEST, resource.getCpuUnits())
                  .set(POD_INFO.MEMORYREQUEST, resource.getMemoryMb())
                  .set(POD_INFO.PARENTNODEID, parentId)
                  .onConflictOnConstraint(Keys.POD_INFO_UNIQUE_RECORD_INDEX)
                  .doUpdate()
                  .set(POD_INFO.STARTTIME, instantToOffsetDateTime(instanceInfo.getUsageStartTime()))
                  .set(POD_INFO.NAMESPACE, namespace)
                  .set(POD_INFO.NAME, instanceInfo.getInstanceName())
                  .set(POD_INFO.WORKLOADID, workloadId)
                  .set(POD_INFO.CPUREQUEST, resource.getCpuUnits())
                  .set(POD_INFO.MEMORYREQUEST, resource.getMemoryMb())
                  .set(POD_INFO.PARENTNODEID, parentId)
                  .set(POD_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET)));
  }

  @Override
  public void updatePodStopEvent(@NotNull List<InstanceEvent> instanceEventList) {
    for (InstanceEvent instanceEvent : instanceEventList) {
      updateOne(dslContext.update(POD_INFO)
                    .set(POD_INFO.STOPTIME, instantToOffsetDateTime(instanceEvent.getTimestamp()))
                    .set(POD_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                    .where(POD_INFO.ACCOUNTID.eq(instanceEvent.getAccountId()),
                        POD_INFO.CLUSTERID.eq(instanceEvent.getClusterId()),
                        POD_INFO.INSTANCEID.eq(instanceEvent.getInstanceId())));
    }
  }

  @Override
  public void updatePodLifecycleEvent(@NotNull String accountId, @NotNull List<Lifecycle> lifecycleList) {
    for (Lifecycle lifecycle : lifecycleList) {
      UpdateSetMoreStep<PodInfoRecord> updateSetMoreStep =
          dslContext.update(POD_INFO).set(POD_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET));

      if (Lifecycle.EventType.EVENT_TYPE_START.equals(lifecycle.getType())) {
        updateSetMoreStep =
            updateSetMoreStep
                .set(POD_INFO.STARTTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET))
                .setNull(POD_INFO.STOPTIME);
      } else {
        updateSetMoreStep = updateSetMoreStep.set(
            POD_INFO.STOPTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET));
      }

      updateOne(updateSetMoreStep.where(POD_INFO.ACCOUNTID.eq(accountId),
          POD_INFO.CLUSTERID.eq(lifecycle.getClusterId()), POD_INFO.INSTANCEID.eq(lifecycle.getInstanceId())));
    }
  }

  @Override
  public void updateNodeStopEvent(@NotNull List<InstanceEvent> instanceEventList) {
    for (InstanceEvent instanceEvent : instanceEventList) {
      updateOne(dslContext.update(NODE_INFO)
                    .set(NODE_INFO.STOPTIME, instantToOffsetDateTime(instanceEvent.getTimestamp()))
                    .set(NODE_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                    .where(NODE_INFO.ACCOUNTID.eq(instanceEvent.getAccountId()),
                        NODE_INFO.CLUSTERID.eq(instanceEvent.getClusterId()),
                        NODE_INFO.INSTANCEID.eq(instanceEvent.getInstanceId())));
    }
  }

  @Override
  public void updateNodeLifecycleEvent(@NotNull String accountId, @NotNull List<Lifecycle> lifecycleList) {
    for (Lifecycle lifecycle : lifecycleList) {
      UpdateSetMoreStep<NodeInfoRecord> updateSetMoreStep =
          dslContext.update(NODE_INFO).set(NODE_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET));

      if (Lifecycle.EventType.EVENT_TYPE_START.equals(lifecycle.getType())) {
        updateSetMoreStep =
            updateSetMoreStep
                .set(NODE_INFO.STARTTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET))
                .setNull(NODE_INFO.STOPTIME);
      } else {
        updateSetMoreStep = updateSetMoreStep.set(
            NODE_INFO.STOPTIME, HTimestamps.toInstant(lifecycle.getTimestamp()).atOffset(ZONE_OFFSET));
      }
      updateOne(updateSetMoreStep.set(NODE_INFO.UPDATEDAT, OffsetDateTime.now(ZONE_OFFSET))
                    .where(NODE_INFO.ACCOUNTID.eq(accountId), NODE_INFO.CLUSTERID.eq(lifecycle.getClusterId()),
                        NODE_INFO.INSTANCEID.eq(lifecycle.getInstanceId())));
    }
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  private static void insertOne(@NotNull InsertFinalStep<? extends Record> finalStep) {
    finalStep.execute();
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  private static void updateOne(@NotNull UpdateFinalStep<? extends Record> finalStep) {
    finalStep.execute();
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  private static <R extends Record> int bulkInsert(
      @NotNull final Table<R> table, @NotNull final List<R> records, @NotNull final DSLContext dslContext) {
    if (records.isEmpty()) {
      return 0;
    }

    InsertSetStep<R> insertStep = dslContext.insertInto(table);
    for (int i = 0; i < records.size() - 1; i++) {
      insertStep = insertStep.set(records.get(i)).newRecord();
    }
    final R lastRecord = records.get(records.size() - 1);

    return insertStep.set(lastRecord).onConflictDoNothing().execute();
  }

  private static OffsetDateTime instantToOffsetDateTime(@Nullable Instant instant) {
    return ofNullable(instant).map(x -> x.atOffset(ZONE_OFFSET)).orElse(null);
  }
}
