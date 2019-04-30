package software.wings.graphql.datafetcher.pipeline;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLPipelinesQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.graphql.schema.type.QLPipelineConnection;
import software.wings.graphql.schema.type.QLPipelineConnection.QLPipelineConnectionBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLPipelineConnection, QLPipelinesQueryParameters> {
  @Inject
  public PipelineConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLPipelineConnection fetch(QLPipelinesQueryParameters qlQuery) {
    final Query<Pipeline> query = persistence.createQuery(Pipeline.class)
                                      .filter(PipelineKeys.appId, qlQuery.getApplicationId())
                                      .order(Sort.descending(PipelineKeys.createdAt));

    QLPipelineConnectionBuilder connectionBuilder = QLPipelineConnection.builder();
    connectionBuilder.pageInfo(populate(qlQuery, query, pipeline -> {
      QLPipelineBuilder builder = QLPipeline.builder();
      PipelineController.populatePipeline(pipeline, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }
}
