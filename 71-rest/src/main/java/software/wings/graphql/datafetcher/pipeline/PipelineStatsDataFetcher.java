package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcherWithTags;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineEntityAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineTagAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineTagType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PipelineStatsDataFetcher
    extends RealTimeStatsDataFetcherWithTags<QLNoOpAggregateFunction, QLPipelineFilter, QLPipelineAggregation,
        QLNoOpSortCriteria, QLPipelineTagType, QLPipelineTagAggregation, QLPipelineEntityAggregation> {
  @Inject PipelineQueryHelper pipelineQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLPipelineFilter> filters,
      List<QLPipelineAggregation> groupByList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Pipeline.class;
    final List<String> groupByEntityList = new ArrayList<>();
    if (isNotEmpty(groupByList)) {
      groupByList.forEach(groupBy -> {
        if (groupBy.getEntityAggregation() != null) {
          groupByEntityList.add(groupBy.getEntityAggregation().name());
        }

        if (groupBy.getTagAggregation() != null) {
          QLPipelineEntityAggregation groupByEntityFromTag = getGroupByEntityFromTag(groupBy.getTagAggregation());
          if (groupByEntityFromTag != null) {
            groupByEntityList.add(groupByEntityFromTag.name());
          }
        }
      });
    }
    return getQLData(accountId, filters, entityClass, groupByEntityList);
  }

  @Override
  public String getAggregationFieldName(String aggregation) {
    QLPipelineEntityAggregation pipelineAggregation = QLPipelineEntityAggregation.valueOf(aggregation);
    switch (pipelineAggregation) {
      case Application:
        return "appId";
      default:
        logger.warn("Unknown aggregation type" + aggregation);
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }

  @Override
  public void populateFilters(String accountId, List<QLPipelineFilter> filters, Query query) {
    pipelineQueryHelper.setQuery(filters, query, accountId);
  }

  @Override
  public String getEntityType() {
    return NameService.pipeline;
  }

  @Override
  protected QLPipelineTagAggregation getTagAggregation(QLPipelineAggregation groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected EntityType getEntityType(QLPipelineTagType entityType) {
    return pipelineQueryHelper.getEntityType(entityType);
  }

  @Override
  protected QLPipelineEntityAggregation getEntityAggregation(QLPipelineAggregation groupBy) {
    return groupBy.getEntityAggregation();
  }

  @Override
  protected QLPipelineEntityAggregation getGroupByEntityFromTag(QLPipelineTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLPipelineEntityAggregation.Application;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }
}
