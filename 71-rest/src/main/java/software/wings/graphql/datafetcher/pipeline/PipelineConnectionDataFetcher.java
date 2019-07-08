package software.wings.graphql.datafetcher.pipeline;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.graphql.schema.type.QLPipelineConnection;
import software.wings.graphql.schema.type.QLPipelineConnection.QLPipelineConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class PipelineConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLPipelineFilter, QLNoOpSortCriteria, QLPipelineConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.PIPELINE, action = Action.READ)
  public QLPipelineConnection fetchConnection(List<QLPipelineFilter> pipelineFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Pipeline> query = populateFilters(wingsPersistence, pipelineFilters, Pipeline.class);
    query.order(Sort.descending(PipelineKeys.createdAt));

    QLPipelineConnectionBuilder connectionBuilder = QLPipelineConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, pipeline -> {
      QLPipelineBuilder builder = QLPipeline.builder();
      PipelineController.populatePipeline(pipeline, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLPipelineFilterType pipelineFilterType = QLPipelineFilterType.valueOf(filterType);
    switch (pipelineFilterType) {
      case Application:
        return PipelineKeys.appId;
      case Pipeline:
        return PipelineKeys.uuid;
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
