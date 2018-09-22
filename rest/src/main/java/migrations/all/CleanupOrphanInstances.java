package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to cleanup instances if inframapping/env/service/app was deleted
 *
 * @author rktummala on 08/01/18
 */
public class CleanupOrphanInstances implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(CleanupOrphanInstances.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      logger.info("Start - Deleting of orphan instances");

      List<Key<Application>> appKeyList = wingsPersistence.createQuery(Application.class).asKeyList();
      Set<String> apps = appKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Service>> serviceKeyList = wingsPersistence.createQuery(Service.class).asKeyList();
      Set<String> services = serviceKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Environment>> envKeyList = wingsPersistence.createQuery(Environment.class).asKeyList();
      Set<String> envs = envKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<InfrastructureMapping>> infraKeyList =
          wingsPersistence.createQuery(InfrastructureMapping.class).asKeyList();
      Set<String> infraMappings = infraKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Instance> instances = wingsPersistence.createQuery(Instance.class).asList();
      Set<String> orphanInstances =
          instances.stream()
              .filter(instance
                  -> !apps.contains(instance.getAppId()) || !services.contains(instance.getServiceId())
                      || !envs.contains(instance.getEnvId()) || !infraMappings.contains(instance.getInfraMappingId()))
              .map(instance -> instance.getUuid())
              .collect(Collectors.toSet());

      logger.info("Number of orphaned instances identified: " + orphanInstances.size());
      Query query = wingsPersistence.createQuery(Instance.class);
      query.field("_id").in(orphanInstances);

      boolean delete = wingsPersistence.delete(query);
      if (delete) {
        logger.info("Deleted orphan instances successfully");
      } else {
        logger.error("Unable to delete orphan instances");
      }

    } catch (Exception ex) {
      logger.error("Error while deleting orphan instances", ex);
    }
  }
}
