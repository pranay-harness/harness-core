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
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.instance.InstanceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to cleanup instances if inframapping/env/service/app was deleted
 *
 * @author rktummala on 08/01/18
 */
@Slf4j
public class CleanupOrphanInstances implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceService instanceService;

  @Override
  public void migrate() {
    try {
      logger.info("Start - Deleting of orphan instances");

      List<Key<Application>> appKeyList = wingsPersistence.createQuery(Application.class, excludeAuthority).asKeyList();
      Set<String> apps = appKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Service>> serviceKeyList = wingsPersistence.createQuery(Service.class, excludeAuthority).asKeyList();
      Set<String> services = serviceKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<Environment>> envKeyList = wingsPersistence.createQuery(Environment.class, excludeAuthority).asKeyList();
      Set<String> envs = envKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<SettingAttribute>> settingAttributeKeyList =
          wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).asKeyList();
      Set<String> settingAttributes =
          settingAttributeKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      List<Key<InfrastructureMapping>> infraKeyList =
          wingsPersistence.createQuery(InfrastructureMapping.class).asKeyList();
      Set<String> infraMappings = infraKeyList.stream().map(key -> (String) key.getId()).collect(Collectors.toSet());

      Query<Instance> query = wingsPersistence.createQuery(Instance.class, excludeAuthority)
                                  .filter(InstanceKeys.isDeleted, false)
                                  .project("_id", true)
                                  .project("appId", true)
                                  .project("envId", true)
                                  .project("serviceId", true)
                                  .project("computeProviderId", true)
                                  .project("infraMappingId", true);

      Set<String> orphanInstances = new HashSet<>();
      try (HIterator<Instance> iterator = new HIterator<>(query.fetch())) {
        for (Instance instance : iterator) {
          if (!apps.contains(instance.getAppId()) || !services.contains(instance.getServiceId())
              || !envs.contains(instance.getEnvId()) || !infraMappings.contains(instance.getInfraMappingId())
              || !settingAttributes.contains(instance.getComputeProviderId())) {
            orphanInstances.add(instance.getUuid());
          }
        }
      }

      logger.info("Number of orphaned instances identified: " + orphanInstances.size());
      instanceService.delete(orphanInstances);
      logger.info("Deleted orphan instances successfully");
    } catch (Exception ex) {
      logger.error("Error while deleting orphan instances", ex);
    }
  }
}
