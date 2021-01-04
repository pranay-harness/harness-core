package io.harness.batch.processing.service.impl;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.intfc.InstanceDataBulkWriteService;
import io.harness.ccm.commons.beans.InstanceState;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.commons.entities.InstanceData.InstanceDataKeys;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.utils.HTimestamps;

import software.wings.dl.WingsPersistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InstanceDataBulkWriteServiceImpl implements InstanceDataBulkWriteService {
  @Autowired private WingsPersistence wingsPersistence;
  @Autowired private BatchMainConfig config;

  private static final Gson GSON = new Gson();

  private static Document objectToDocument(Object obj) {
    return Document.parse(GSON.toJson(obj));
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean updateList(List<?> objectList) {
    final int bulkWriteLimit = config.getBatchQueryConfig().getQueryBatchSize();
    boolean result = true;

    // Lists.partition always returns non empty List
    for (List<?> objects : Lists.partition(objectList, bulkWriteLimit)) {
      Object obj = objects.get(0);
      if (obj instanceof Lifecycle) {
        result = updateLifecyclesInternal((List<Lifecycle>) objects);
      } else if (obj instanceof InstanceInfo) {
        result = createInfosInternal((List<InstanceInfo>) objects);
      } else if (obj instanceof InstanceEvent) {
        result = updateEventsInternal((List<InstanceEvent>) objects);
      } else {
        throw new NotImplementedException(format("Please implement %s update method in %s",
            obj.getClass().getSimpleName(), this.getClass().getSimpleName()));
      }
      if (!result) {
        return false;
      }
    }
    return result;
  }

  private boolean updateLifecyclesInternal(List<Lifecycle> lifecycleList) {
    Preconditions.checkNotNull(wingsPersistence);
    final BulkWriteOperation bulkWriteOperation =
        wingsPersistence.getCollection(InstanceData.class).initializeUnorderedBulkOperation();

    for (Lifecycle lifecycle : lifecycleList) {
      try {
        Instant instanceTime = HTimestamps.toInstant(lifecycle.getTimestamp());

        BasicDBObject filter = new BasicDBObject(ImmutableMap.of(InstanceDataKeys.instanceId, lifecycle.getInstanceId(),
            InstanceDataKeys.instanceState, InstanceState.RUNNING.name(), InstanceDataKeys.usageStartTime,
            new BasicDBObject("$lte", instanceTime)));

        BasicDBObject updateOperations = new BasicDBObject(ImmutableMap.of(InstanceDataKeys.usageStopTime, instanceTime,
            InstanceDataKeys.instanceState, InstanceState.STOPPED.name()));
        updateOperations.append(InstanceDataKeys.ttl, new Date(instanceTime.plus(180, ChronoUnit.DAYS).toEpochMilli()));

        bulkWriteOperation.find(filter).update(new BasicDBObject("$set", updateOperations));
      } catch (Exception ex) {
        log.error("Error updating syncEvent {}", lifecycle.toString(), ex);
      }
    }

    BulkWriteResult result = bulkWriteExecutor(bulkWriteOperation);
    return result.isAcknowledged();
  }

  private boolean createInfosInternal(List<InstanceInfo> instanceInfos) {
    BulkWriteOperation bulkWriteOperation =
        wingsPersistence.getCollection(InstanceData.class).initializeUnorderedBulkOperation();

    for (InstanceInfo instanceInfo : instanceInfos) {
      try {
        BasicDBObject instanceInfoDocument =
            new BasicDBObject()
                .append(InstanceDataKeys.accountId, instanceInfo.getAccountId())
                .append(InstanceDataKeys.settingId, instanceInfo.getSettingId())
                .append(InstanceDataKeys.instanceId, instanceInfo.getInstanceId())
                .append(InstanceDataKeys.instanceName, instanceInfo.getInstanceName())
                .append(InstanceDataKeys.instanceType, instanceInfo.getInstanceType().name())
                .append(InstanceDataKeys.clusterId, instanceInfo.getClusterId())
                .append(InstanceDataKeys.clusterName, instanceInfo.getClusterName())
                .append(InstanceDataKeys.instanceState, instanceInfo.getInstanceState().name())
                .append(InstanceDataKeys.usageStartTime, instanceInfo.getUsageStartTime());

        if (!isNull(instanceInfo.getResource())) {
          instanceInfoDocument.append(InstanceDataKeys.totalResource, objectToDocument(instanceInfo.getResource()));
        }

        if (!isNull(instanceInfo.getCloudProviderInstanceId())) {
          instanceInfoDocument.append(
              InstanceDataKeys.cloudProviderInstanceId, instanceInfo.getCloudProviderInstanceId());
        }

        if (!isNull(instanceInfo.getResourceLimit())) {
          instanceInfoDocument.append(
              InstanceDataKeys.limitResource, objectToDocument(instanceInfo.getResourceLimit()));
        }

        if (!isNull(instanceInfo.getAllocatableResource())) {
          instanceInfoDocument.append(
              InstanceDataKeys.allocatableResource, objectToDocument(instanceInfo.getAllocatableResource()));
        }

        if (!isNull(instanceInfo.getStorageResource())) {
          instanceInfoDocument.append(
              InstanceDataKeys.storageResource, objectToDocument(instanceInfo.getStorageResource()));
        }

        if (!isNull(instanceInfo.getLabels())) {
          instanceInfoDocument.append(InstanceDataKeys.labels, instanceInfo.getLabels());
        }

        if (!isNull(instanceInfo.getNamespaceLabels()) && !instanceInfo.getNamespaceLabels().isEmpty()) {
          instanceInfoDocument.append(InstanceDataKeys.namespaceLabels, instanceInfo.getNamespaceLabels());
        }

        if (!isNull(instanceInfo.getMetaData())) {
          instanceInfoDocument.append(InstanceDataKeys.metaData, instanceInfo.getMetaData());
        }

        if (!isNull(instanceInfo.getHarnessServiceInfo())) {
          instanceInfoDocument.append(
              InstanceDataKeys.harnessServiceInfo, objectToDocument(instanceInfo.getHarnessServiceInfo()));
        }

        final BasicDBObject filter = new BasicDBObject(
            ImmutableMap.of(InstanceDataKeys.accountId, instanceInfo.getAccountId(), InstanceDataKeys.clusterId,
                instanceInfo.getClusterId(), InstanceDataKeys.instanceId, instanceInfo.getInstanceId()));

        bulkWriteOperation.find(filter).upsert().update(new BasicDBObject("$set", instanceInfoDocument));
      } catch (Exception ex) {
        log.error("Error creating instanceInfo query{}", instanceInfo.toString(), ex);
      }
    }

    BulkWriteResult result = bulkWriteExecutor(bulkWriteOperation);
    return result.isAcknowledged();
  }

  // The update events is Unordered to utilize mongo parallel threads
  private boolean updateEventsInternal(List<InstanceEvent> instanceEvents) {
    BulkWriteOperation bulkWriteOperation =
        wingsPersistence.getCollection(InstanceData.class).initializeUnorderedBulkOperation();

    for (InstanceEvent instanceEvent : instanceEvents) {
      try {
        BasicDBObject updateOperations;
        BasicDBObject filter = new BasicDBObject(
            ImmutableMap.of(InstanceDataKeys.accountId, instanceEvent.getAccountId(), InstanceDataKeys.clusterId,
                instanceEvent.getClusterId(), InstanceDataKeys.instanceId, instanceEvent.getInstanceId()));

        Instant instant = instanceEvent.getTimestamp();

        Preconditions.checkNotNull(instanceEvent.getType(), "InstanceEvent Type is null");
        switch (instanceEvent.getType()) {
          case STOP:
            filter.append(InstanceDataKeys.instanceState,
                new BasicDBObject(
                    "$in", ImmutableList.of(InstanceState.RUNNING.name(), InstanceState.INITIALIZING.name())));

            updateOperations = new BasicDBObject(ImmutableMap.of(
                InstanceDataKeys.usageStopTime, instant, InstanceDataKeys.instanceState, InstanceState.STOPPED.name()));

            if (instanceEvent.getInstanceType() == InstanceType.K8S_POD) {
              updateOperations.append(InstanceDataKeys.ttl, new Date(instant.plus(30, ChronoUnit.DAYS).toEpochMilli()));
            } else if (instanceEvent.getInstanceType() == InstanceType.K8S_NODE) {
              updateOperations.append(
                  InstanceDataKeys.ttl, new Date(instant.plus(180, ChronoUnit.DAYS).toEpochMilli()));
            }

            bulkWriteOperation.find(filter).update(new BasicDBObject("$set", updateOperations));
            break;

          // Deprecated, we have combined (Info & Start) to (Info)
          case START:
            filter.append(InstanceDataKeys.instanceState, InstanceState.INITIALIZING.name());

            updateOperations = new BasicDBObject(ImmutableMap.of(InstanceDataKeys.usageStartTime, instant,
                InstanceDataKeys.instanceState, InstanceState.RUNNING.name()));

            bulkWriteOperation.find(filter).update(new BasicDBObject("$set", updateOperations));
            break;
          default:
            break;
        }
      } catch (Exception ex) {
        log.error("Error creating instanceEvent query{}", instanceEvent.toString(), ex);
      }
    }

    BulkWriteResult result = bulkWriteExecutor(bulkWriteOperation);
    return result.isAcknowledged();
  }

  private static BulkWriteResult bulkWriteExecutor(BulkWriteOperation bulkWriteOperation) {
    BulkWriteResult result;
    for (int i = 1; i < 5; i++) {
      try {
        result = bulkWriteOperation.execute();
        log.info("BulkWriteExecutor result: {}", result.toString());
        return result;
      } catch (IllegalArgumentException ex) {
        throw ex;
      } catch (Exception ex) {
        log.warn("Exception occurred with bulkWriteExecutor, retry:{}", i, ex);
      }
    }
    result = bulkWriteOperation.execute();
    log.info("BulkWriteExecutor result: {}", result.toString());
    return result;
  }
}
