package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilterType;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InstanceStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLInstanceFilter,
    QLInstanceAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLInstanceFilter> filters,
      List<QLInstanceAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.filter("accountId", accountId);
    query.filter("isDeleted", false);

    if (isNotEmpty(filters)) {
      filters.forEach(filter -> {
        QLStringFilter stringFilter = filter.getStringFilter();
        QLNumberFilter numberFilter = filter.getNumberFilter();

        if (stringFilter != null && numberFilter != null) {
          throw new WingsException("Only one filter should be set");
        }

        if (stringFilter == null && numberFilter == null) {
          throw new WingsException("All filters are null");
        }

        QLInstanceFilterType filterType = filter.getType();
        String columnName = getFieldName(filterType);
        FieldEnd<? extends Query<Instance>> field = query.field(columnName);

        if (stringFilter != null) {
          setStringFilter(field, stringFilter);
        }

        if (numberFilter != null) {
          setNumberFilter(field, numberFilter);
        }
      });
    }

    if (isNotEmpty(groupBy)) {
      if (groupBy.size() == 1) {
        QLInstanceAggregation firstLevelAggregation = groupBy.get(0);
        String entityIdColumn = getFieldName(firstLevelAggregation);
        String entityNameColumn = getNameField(firstLevelAggregation);
        String function = getAggregateFunction(aggregateFunction);
        List<QLDataPoint> dataPoints = new ArrayList<>();

        wingsPersistence.getDatastore(Instance.class)
            .createAggregation(Instance.class)
            .match(query)
            .group(Group.id(grouping(entityIdColumn)), grouping("count", accumulator(function, 1)),
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
        QLInstanceAggregation firstLevelAggregation = groupBy.get(0);
        QLInstanceAggregation secondLevelAggregation = groupBy.get(1);
        String entityIdColumn = getFieldName(firstLevelAggregation);
        String entityNameColumn = getNameField(firstLevelAggregation);
        String secondLevelEntityIdColumn = getFieldName(secondLevelAggregation);
        String secondLevelEntityNameColumn = getNameField(secondLevelAggregation);
        String function = getAggregateFunction(aggregateFunction);

        List<TwoLevelAggregatedData> aggregatedDataList = new ArrayList<>();
        wingsPersistence.getDatastore(query.getEntityClass())
            .createAggregation(Instance.class)
            .match(query)
            .group(Group.id(grouping(entityIdColumn), grouping(secondLevelEntityIdColumn)),
                grouping("count", accumulator(function, 1)),
                grouping("firstLevelInfo",
                    grouping("$first", projection("id", entityIdColumn), projection("name", entityNameColumn))),
                grouping("secondLevelInfo",
                    grouping("$first", projection("id", secondLevelEntityIdColumn),
                        projection("name", secondLevelEntityNameColumn))))
            .sort(
                ascending("_id." + entityIdColumn), ascending("_id." + secondLevelEntityIdColumn), descending("count"))
            .aggregate(TwoLevelAggregatedData.class)
            .forEachRemaining(twoLevelAggregatedData -> { aggregatedDataList.add(twoLevelAggregatedData); });

        return getStackedData(aggregatedDataList, firstLevelAggregation.name(), secondLevelAggregation.name());

      } else {
        throw new WingsException("Only one or two level aggregations supported right now");
      }

    } else {
      return getSingleDataPointData(query);
    }
  }

  private String getFieldName(QLInstanceFilterType filterType) {
    switch (filterType) {
      case CreatedAt:
        return "createdAt";
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
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  private String getFieldName(QLInstanceAggregation aggregation) {
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
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  private String getNameField(QLInstanceAggregation aggregation) {
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
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }
}
