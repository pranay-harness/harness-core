package software.wings.graphql.datafetcher.environment.batch;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.environment.EnvironmentController;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentBatchDataLoader implements MappedBatchLoader<String, QLEnvironment> {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public CompletionStage<Map<String, QLEnvironment>> load(Set<String> environmentIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLEnvironment> environmentMap = null;
      if (!CollectionUtils.isEmpty(environmentIds)) {
        environmentMap = getEnvironmentMap(environmentIds);
      } else {
        environmentMap = Collections.EMPTY_MAP;
      }
      return environmentMap;
    });
  }

  public Map<String, QLEnvironment> getEnvironmentMap(@NotNull Set<String> environmentIds) {
    Query<Environment> query = wingsPersistence.createQuery(Environment.class, HQuery.excludeAuthority)
                                   .field(EnvironmentKeys.uuid)
                                   .in(environmentIds);
    Map<String, QLEnvironment> environmentMap = new HashMap<>();

    try (HIterator<Environment> environments = new HIterator<>(query.fetch())) {
      environments.forEach(environment -> {
        final QLEnvironmentBuilder builder = QLEnvironment.builder();
        EnvironmentController.populateEnvironment(environment, builder);
        environmentMap.put(environment.getUuid(), builder.build());
      });
    }
    return environmentMap;
  }
}
