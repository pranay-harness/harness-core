package software.wings.graphql.datafetcher.pipeline.batch;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.pipeline.PipelineController;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PipelineBatchDataLoader implements MappedBatchLoader<String, QLPipeline> {
  @Inject private WingsPersistence wingsPersistence;

  @Inject
  public PipelineBatchDataLoader(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public CompletionStage<Map<String, QLPipeline>> load(Set<String> pipelineIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLPipeline> pipelineMap = null;
      if (!CollectionUtils.isEmpty(pipelineIds)) {
        pipelineMap = getPipelineMap(pipelineIds);
      } else {
        pipelineMap = Collections.EMPTY_MAP;
      }
      return pipelineMap;
    });
  }

  public Map<String, QLPipeline> getPipelineMap(@NotNull Set<String> pipelineIds) {
    Query<Pipeline> query =
        wingsPersistence.createQuery(Pipeline.class, HQuery.excludeAuthority).field(PipelineKeys.uuid).in(pipelineIds);
    Map<String, QLPipeline> pipelineMap = new HashMap<>();

    try (HIterator<Pipeline> pipelines = new HIterator<>(query.fetch())) {
      pipelines.forEach(pipeline -> {
        final QLPipelineBuilder builder = QLPipeline.builder();
        PipelineController.populatePipeline(pipeline, builder);
        pipelineMap.put(pipeline.getUuid(), builder.build());
      });
    }
    return pipelineMap;
  }
}
