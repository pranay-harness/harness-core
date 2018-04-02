package software.wings.integration.migration.legacy;

import static java.util.Arrays.asList;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Collection;
import java.util.List;

/**
 * @author rktummala on 12/14/17
 */
@Integration
@Ignore
public class InstanceDuplicateCleanupMigratorUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(InstanceDuplicateCleanupMigratorUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;

  @Test
  public void cleanupInstancesByService() {
    Query<Application> query = wingsPersistence.createQuery(Application.class);
    List<Application> apps = query.project("_id", true).asList();
    apps.stream().forEach(app -> {
      PageRequest<Service> servicePageRequest =
          aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, app.getUuid()).build();
      PageResponse<Service> services = serviceResourceService.list(servicePageRequest, false, false);
      services.stream().forEach(service -> deleteDuplicates(service.getUuid()));
    });
  }

  private void deleteDuplicates(String serviceId) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    List<Instance> instances = query.filter("serviceId", serviceId)
                                   .field("instanceType")
                                   .in(asList("KUBERNETES_CONTAINER_INSTANCE"))
                                   .project("containerInstanceKey.containerId", true)
                                   .project("_id", true)
                                   .asList();
    Multimap multiMap = HashMultimap.create();
    instances.stream().forEach(instance -> {
      String uuid = instance.getUuid();
      String containerId = instance.getContainerInstanceKey().getContainerId();
      multiMap.put(containerId, uuid);
    });

    List<String> idsToDelete = Lists.newArrayList();

    multiMap.keySet().stream().forEach(key -> {
      Collection collection = multiMap.get(key);
      if (collection.size() > 1) {
        idsToDelete.addAll(collection);
        idsToDelete.remove(idsToDelete.size() - 1);
      }
    });

    int size = idsToDelete.size();
    if (size > 1) {
      logger.info("Duplicate count for service: " + serviceId + " size:" + size);
    }

    if (!idsToDelete.isEmpty()) {
      Query<Instance> deleteQuery = wingsPersistence.createQuery(Instance.class);
      deleteQuery.field("_id").in(idsToDelete);
      wingsPersistence.delete(deleteQuery);
    }
  }
}
