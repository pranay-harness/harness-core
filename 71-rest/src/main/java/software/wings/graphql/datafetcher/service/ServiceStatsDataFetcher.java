package software.wings.graphql.datafetcher.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcherWithTags;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.service.QLServiceAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceEntityAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceTagAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceTagType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ServiceStatsDataFetcher extends RealTimeStatsDataFetcherWithTags<QLNoOpAggregateFunction, QLServiceFilter,
    QLServiceAggregation, QLNoOpSortCriteria, QLServiceTagType, QLServiceTagAggregation, QLServiceEntityAggregation> {
  @Inject ServiceQueryHelper serviceQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLServiceFilter> filters,
      List<QLServiceAggregation> groupByList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Service.class;
    final List<String> groupByEntityList = new ArrayList<>();
    if (isNotEmpty(groupByList)) {
      groupByList.forEach(groupBy -> {
        if (groupBy.getEntityAggregation() != null) {
          groupByEntityList.add(groupBy.getEntityAggregation().name());
        }

        if (groupBy.getTagAggregation() != null) {
          QLServiceEntityAggregation groupByEntityFromTag = getGroupByEntityFromTag(groupBy.getTagAggregation());
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
    QLServiceEntityAggregation serviceAggregation = QLServiceEntityAggregation.valueOf(aggregation);
    switch (serviceAggregation) {
      case Application:
        return "appId";
      case ArtifactType:
        return "artifactType";
      default:
        logger.warn("Unknown aggregation type" + aggregation);
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }

  @Override
  public void populateFilters(String accountId, List<QLServiceFilter> filters, Query query) {
    serviceQueryHelper.setQuery(filters, query, accountId);
  }

  @Override
  public String getEntityType() {
    return NameService.service;
  }

  @Override
  protected QLServiceTagAggregation getTagAggregation(QLServiceAggregation groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected EntityType getEntityType(QLServiceTagType entityType) {
    return serviceQueryHelper.getEntityType(entityType);
  }

  @Override
  protected QLServiceEntityAggregation getEntityAggregation(QLServiceAggregation groupBy) {
    return groupBy.getEntityAggregation();
  }

  @Override
  protected QLServiceEntityAggregation getGroupByEntityFromTag(QLServiceTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLServiceEntityAggregation.Application;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }
}
