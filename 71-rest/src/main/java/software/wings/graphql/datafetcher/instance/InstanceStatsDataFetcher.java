package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceEntityAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class InstanceStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLInstanceFilter,
    QLInstanceAggregation, QLNoOpSortCriteria> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject InstanceTimeSeriesDataHelper timeSeriesDataHelper;
  @Inject InstanceQueryHelper instanceMongoHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLInstanceFilter> filters,
      List<QLInstanceAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    validateAggregations(groupBy);

    /**
     * Cases to be handled
     * 1) No agg
     * 2) 1 entity
     * 3) 1 entity, 1 time
     * 4) 1 entity, 1 tag
     * 5) 1 time agg, 1 tag agg
     * 6) 2 entity agg
     * 7) 2 tag agg
     * 8) 1 tag agg, 1 entity agg
     * 9) 1 tag agg, 1 time agg
     * 10) 1 tag agg
     * 11) 1 time agg
     * 12)
     * 13)
     * 14)
     * 15)
     * 16)
     * 17)
     * 18)
     */

    QLTimeSeriesAggregation groupByTime = getGroupByTime(groupBy);
    List<QLInstanceEntityAggregation> groupByEntity = getGroupByEntity(groupBy);

    if (groupByTime != null) {
      if (isNotEmpty(groupByEntity)) {
        if (groupByEntity.size() == 1) {
          return timeSeriesDataHelper.getTimeSeriesAggregatedData(
              accountId, aggregateFunction, filters, groupByTime, groupByEntity.get(0));
        } else {
          throw new InvalidRequestException("Invalid query. Only one groupBy column allowed");
        }
      } else {
        return timeSeriesDataHelper.getTimeSeriesData(accountId, aggregateFunction, filters, groupByTime);
      }
    } else {
      if (isNotEmpty(filters)) {
        Optional<QLInstanceFilter> timeFilter =
            filters.stream().filter(filter -> filter.getCreatedAt() != null).findFirst();
        if (timeFilter.isPresent()) {
          throw new InvalidRequestException(
              "Time Filter is only supported for time series data (grouped by time)", WingsException.USER);
        }
      }

      Query<Instance> query = wingsPersistence.createQuery(Instance.class);
      query.filter("accountId", accountId);
      query.filter("isDeleted", false);

      instanceMongoHelper.setQuery(accountId, filters, query);

      if (isNotEmpty(groupBy)) {
        if (groupBy.size() == 1) {
          QLInstanceEntityAggregation firstLevelAggregation = groupByEntity.get(0);
          String entityIdColumn = getMongoFieldName(firstLevelAggregation);
          String entityNameColumn = getNameField(firstLevelAggregation);
          List<QLDataPoint> dataPoints = new ArrayList<>();

          wingsPersistence.getDatastore(Instance.class)
              .createAggregation(Instance.class)
              .match(query)
              .group(Group.id(grouping(entityIdColumn)), grouping("count", accumulator("$sum", 1)),
                  grouping(entityNameColumn, grouping("$first", entityNameColumn)))
              .project(projection("_id").suppress(), projection("entityId", "_id." + entityIdColumn),
                  projection("entityName", entityNameColumn), projection("count"))
              .aggregate(FlatEntitySummaryStats.class)
              .forEachRemaining(flatEntitySummaryStats -> {
                QLDataPoint dataPoint = getDataPoint(flatEntitySummaryStats, firstLevelAggregation.name());
                dataPoints.add(dataPoint);
              });
          return QLAggregatedData.builder().dataPoints(dataPoints).build();
        } else if (groupBy.size() == 2) {
          QLInstanceEntityAggregation firstLevelAggregation = groupByEntity.get(0);
          QLInstanceEntityAggregation secondLevelAggregation = groupByEntity.get(1);
          String entityIdColumn = getMongoFieldName(firstLevelAggregation);
          String entityNameColumn = getNameField(firstLevelAggregation);
          String secondLevelEntityIdColumn = getMongoFieldName(secondLevelAggregation);
          String secondLevelEntityNameColumn = getNameField(secondLevelAggregation);

          List<TwoLevelAggregatedData> aggregatedDataList = new ArrayList<>();
          wingsPersistence.getDatastore(query.getEntityClass())
              .createAggregation(Instance.class)
              .match(query)
              .group(Group.id(grouping(entityIdColumn), grouping(secondLevelEntityIdColumn)),
                  grouping("count", accumulator("$sum", 1)),
                  grouping("firstLevelInfo",
                      grouping("$first", projection("id", entityIdColumn), projection("name", entityNameColumn))),
                  grouping("secondLevelInfo",
                      grouping("$first", projection("id", secondLevelEntityIdColumn),
                          projection("name", secondLevelEntityNameColumn))))
              .sort(ascending("_id." + entityIdColumn), ascending("_id." + secondLevelEntityIdColumn),
                  descending("count"))
              .aggregate(TwoLevelAggregatedData.class)
              .forEachRemaining(twoLevelAggregatedData -> { aggregatedDataList.add(twoLevelAggregatedData); });

          return getStackedData(aggregatedDataList, firstLevelAggregation.name(), secondLevelAggregation.name());

        } else {
          throw new InvalidRequestException("Only one or two level aggregations supported right now");
        }
      } else {
        long count = query.count();
        return QLSinglePointData.builder().dataPoint(QLDataPoint.builder().value(count).build()).build();
      }
    }
  }

  private QLTimeSeriesAggregation getGroupByTime(List<QLInstanceAggregation> groupBy) {
    if (groupBy != null) {
      Optional<QLTimeSeriesAggregation> first = groupBy.stream()
                                                    .filter(g -> g.getTimeAggregation() != null)
                                                    .map(QLInstanceAggregation::getTimeAggregation)
                                                    .findFirst();
      if (first.isPresent()) {
        return first.get();
      }
    }
    return null;
  }

  private List<QLInstanceEntityAggregation> getGroupByEntity(List<QLInstanceAggregation> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityAggregation() != null)
                                 .map(QLInstanceAggregation::getEntityAggregation)
                                 .collect(Collectors.toList())
                           : null;
  }

  private void validateAggregations(List<QLInstanceAggregation> groupBy) {
    // TODO
  }

  @Override
  protected void populateFilters(String accountId, List<QLInstanceFilter> filters, Query query) {
    instanceMongoHelper.setQuery(accountId, filters, query);
  }

  private String getMongoFieldName(QLInstanceEntityAggregation aggregation) {
    switch (aggregation) {
      case Application:
        return "appId";
      case Service:
        return "serviceId";
      case Environment:
        return "envId";
      case CloudProvider:
        return "computeProviderId";
      case InstanceType:
        return "instanceType";
      default:
        throw new InvalidRequestException("Unknown aggregation type" + aggregation);
    }
  }

  private String getNameField(QLInstanceEntityAggregation aggregation) {
    switch (aggregation) {
      case Application:
        return "appName";
      case Service:
        return "serviceName";
      case Environment:
        return "envName";
      case CloudProvider:
        return "computeProviderName";
      case InstanceType:
        return "instanceType";
      default:
        throw new InvalidRequestException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected String getAggregationFieldName(String aggregation) {
    return null;
  }

  @Override
  public String getEntityType() {
    return NameService.instance;
  }
}
