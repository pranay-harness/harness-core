package software.wings.graphql.datafetcher.environment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcherWithTags;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentEntityAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTagAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTagType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EnvironmentStatsDataFetcher
    extends RealTimeStatsDataFetcherWithTags<QLNoOpAggregateFunction, QLEnvironmentFilter, QLEnvironmentAggregation,
        QLNoOpSortCriteria, QLEnvironmentTagType, QLEnvironmentTagAggregation, QLEnvironmentEntityAggregation> {
  @Inject EnvironmentQueryHelper environmentQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLEnvironmentFilter> filters,
      List<QLEnvironmentAggregation> groupByList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Environment.class;
    final List<String> groupByEntityList = new ArrayList<>();
    if (isNotEmpty(groupByList)) {
      groupByList.forEach(groupBy -> {
        if (groupBy.getEntityAggregation() != null) {
          groupByEntityList.add(groupBy.getEntityAggregation().name());
        }

        if (groupBy.getTagAggregation() != null) {
          QLEnvironmentEntityAggregation groupByEntityFromTag = getGroupByEntityFromTag(groupBy.getTagAggregation());
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
    QLEnvironmentEntityAggregation qlEnvironmentAggregation = QLEnvironmentEntityAggregation.valueOf(aggregation);
    switch (qlEnvironmentAggregation) {
      case Application:
        return "appId";
      case EnvironmentType:
        return "environmentType";
      default:
        logger.warn("Unknown aggregation type" + aggregation);
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }

  @Override
  public void populateFilters(String accountId, List<QLEnvironmentFilter> filters, Query query) {
    environmentQueryHelper.setQuery(filters, query, accountId);
  }

  @Override
  public String getEntityType() {
    return NameService.environment;
  }

  @Override
  protected QLEnvironmentTagAggregation getTagAggregation(QLEnvironmentAggregation groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected EntityType getEntityType(QLEnvironmentTagType entityType) {
    return environmentQueryHelper.getEntityType(entityType);
  }

  @Override
  protected QLEnvironmentEntityAggregation getEntityAggregation(QLEnvironmentAggregation groupBy) {
    return groupBy.getEntityAggregation();
  }

  @Override
  protected QLEnvironmentEntityAggregation getGroupByEntityFromTag(QLEnvironmentTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLEnvironmentEntityAggregation.Application;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }
}
