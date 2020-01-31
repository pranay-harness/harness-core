package software.wings.graphql.datafetcher.service.batch;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.service.ServiceController;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceBatchDataLoader implements MappedBatchLoader<String, QLService> {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public CompletionStage<Map<String, QLService>> load(Set<String> serviceIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLService> serviceMap = null;
      if (!CollectionUtils.isEmpty(serviceIds)) {
        serviceMap = getServiceMap(serviceIds);
      } else {
        serviceMap = Collections.EMPTY_MAP;
      }
      return serviceMap;
    });
  }

  public Map<String, QLService> getServiceMap(@NotNull Set<String> serviceIds) {
    Query<Service> query =
        wingsPersistence.createQuery(Service.class, HQuery.excludeAuthority).field(ServiceKeys.uuid).in(serviceIds);
    Map<String, QLService> serviceMap = new HashMap<>();

    try (HIterator<Service> services = new HIterator<>(query.fetch())) {
      services.forEach(service -> {
        final QLServiceBuilder builder = QLService.builder();
        ServiceController.populateService(service, builder);
        serviceMap.put(service.getUuid(), builder.build());
      });
    }
    return serviceMap;
  }
}
