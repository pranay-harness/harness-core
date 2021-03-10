package io.harness.cvng.core.jobs;

import static io.harness.cvng.beans.DataCollectionExecutionStatus.QUEUED;
import static io.harness.cvng.beans.DataCollectionExecutionStatus.RUNNING;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.persistence.HPersistence;

import com.google.api.client.util.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateResults;

@Singleton
@Slf4j
public class MonitoringSourceMigrationHandler
    implements MongoPersistenceIterator.Handler<MonitoringSourcePerpetualTask> {
  @Inject private HPersistence hPersistence;
  @Override
  public void handle(MonitoringSourcePerpetualTask entity) {
    if (isNotEmpty(entity.getDataCollectionWorkerId())) {
      log.info("Migrating monitoring source {}", entity.getUuid());
      String oldWorkerId = getOldWorkerId(entity);
      UpdateResults updateResults =
          hPersistence.update(hPersistence.createQuery(DataCollectionTask.class)
                                  .filter(DataCollectionTaskKeys.dataCollectionWorkerId, oldWorkerId)
                                  .filter(DataCollectionTaskKeys.type, DataCollectionTask.Type.SERVICE_GUARD)
                                  .field(DataCollectionTaskKeys.status)
                                  .in(Arrays.asList(QUEUED, RUNNING)),
              hPersistence.createUpdateOperations(DataCollectionTask.class)
                  .set(DataCollectionTaskKeys.dataCollectionWorkerId, entity.getDataCollectionWorkerId()));
      log.info("Updated data collections tasks for worker ID: {} to newWorkerId {}, updateResults: {}", oldWorkerId,
          entity.getDataCollectionWorkerId(), updateResults.getUpdatedCount());
      log.info("Migrating done monitoring source {}", entity.getUuid());
    }
  }

  private String getOldWorkerId(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask) {
    return getWorkerId(monitoringSourcePerpetualTask.getAccountId(), monitoringSourcePerpetualTask.getOrgIdentifier(),
        monitoringSourcePerpetualTask.getProjectIdentifier(), monitoringSourcePerpetualTask.getConnectorIdentifier(),
        monitoringSourcePerpetualTask.getMonitoringSourceIdentifier());
  }

  private String getWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier) {
    StringJoiner stringJoiner = new StringJoiner(":")
                                    .add(accountId)
                                    .add(orgIdentifier)
                                    .add(projectIdentifier)
                                    .add(monitoringSourceIdentifier)
                                    .add(connectorIdentifier);
    return UUID.nameUUIDFromBytes(stringJoiner.toString().getBytes(Charsets.UTF_8)).toString();
  }
}
