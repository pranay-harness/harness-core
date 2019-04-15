package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.instance.InstanceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to cleanup sync status if inframapping/env/service/app was deleted
 *
 * @author rktummala on 03/19/19
 */
@Slf4j
public class CleanupSyncStatusForDeletedEntities implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceService instanceService;

  @Override
  public void migrate() {
    try {
      logger.info("Start - Deleting of orphan infra sync");

      List<Key<Application>> appKeyList = wingsPersistence.createQuery(Application.class, excludeAuthority).asKeyList();
      Set<String> apps = appKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Service>> serviceKeyList = wingsPersistence.createQuery(Service.class, excludeAuthority).asKeyList();
      Set<String> services = serviceKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Environment>> envKeyList = wingsPersistence.createQuery(Environment.class, excludeAuthority).asKeyList();
      Set<String> envs = envKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<InfrastructureMapping>> infraKeyList =
          wingsPersistence.createQuery(InfrastructureMapping.class).asKeyList();
      Set<String> infraMappings = infraKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      Query<SyncStatus> query = wingsPersistence.createQuery(SyncStatus.class, excludeAuthority)
                                    .project("_id", true)
                                    .project("appId", true)
                                    .project("envId", true)
                                    .project("serviceId", true)
                                    .project("infraMappingId", true);

      Set<String> orphanInfraSyncSet = new HashSet<>();
      try (HIterator<SyncStatus> iterator = new HIterator<>(query.fetch())) {
        while (iterator.hasNext()) {
          SyncStatus infraSyncStatus = iterator.next();
          if (!apps.contains(infraSyncStatus.getAppId()) || !services.contains(infraSyncStatus.getServiceId())
              || !envs.contains(infraSyncStatus.getEnvId())
              || !infraMappings.contains(infraSyncStatus.getInfraMappingId())) {
            orphanInfraSyncSet.add(infraSyncStatus.getUuid());
          }
        }
      }

      logger.info("Number of orphaned infra sync identified: " + orphanInfraSyncSet.size());
      Query<SyncStatus> deleteQuery = wingsPersistence.createQuery(SyncStatus.class, excludeAuthority);
      deleteQuery.field("_id").in(orphanInfraSyncSet);
      wingsPersistence.delete(deleteQuery);
      logger.info("Deleted orphan infra sync  successfully");
    } catch (Exception ex) {
      logger.error("Error while deleting orphan infra sync info", ex);
    }
  }
}
