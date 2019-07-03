package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.instance.dashboard.EntitySummary;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLFilterType;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilterType;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;
import software.wings.graphql.utils.nameservice.NameResult;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class RealTimeStatsDataFetcher<A, F, G, T, S> extends AbstractStatsDataFetcher<A, F, G, T, S> {
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected NameService nameService;

  protected QLData getStackedData(List<String> groupBy, Class entityClass, Query query) {
    String firstLevelAggregation = groupBy.get(0);
    String secondLevelAggregation = groupBy.get(1);
    String entityIdColumn = getAggregationFieldName(firstLevelAggregation);
    String secondLevelEntityIdColumn = getAggregationFieldName(secondLevelAggregation);

    List<TwoLevelAggregatedData> aggregatedDataList = new ArrayList<>();
    wingsPersistence.getDatastore(query.getEntityClass())
        .createAggregation(entityClass)
        .match(query)
        .group(Group.id(grouping(entityIdColumn), grouping(secondLevelEntityIdColumn)),
            grouping("count", new Accumulator("$sum", 1)),
            grouping("firstLevelInfo",
                grouping("$first", projection("id", entityIdColumn), projection("name", entityIdColumn))),
            grouping("secondLevelInfo",
                grouping("$first", projection("id", secondLevelEntityIdColumn),
                    projection("name", secondLevelEntityIdColumn))))
        .sort(ascending("_id." + entityIdColumn), ascending("_id." + secondLevelEntityIdColumn), descending("count"))
        .aggregate(TwoLevelAggregatedData.class)
        .forEachRemaining(aggregatedDataList::add);

    return getStackedData(aggregatedDataList, firstLevelAggregation, secondLevelAggregation);
  }

  protected QLAggregatedData getAggregatedData(List<String> groupBy, Class entityClass, Query query) {
    String firstLevelAggregation = groupBy.get(0);
    String entityIdColumn = getAggregationFieldName(firstLevelAggregation);
    List<QLDataPoint> dataPoints = new ArrayList<>();
    List<FlatEntitySummaryStats> summaryStats = new ArrayList<>();
    wingsPersistence.getDatastore(entityClass)
        .createAggregation(entityClass)
        .match(query)
        .group(Group.id(grouping(entityIdColumn)), grouping("count", new Accumulator("$sum", 1)))
        .project(projection("_id").suppress(), projection("entityId", "_id." + entityIdColumn),
            projection("entityName", "_id." + entityIdColumn), projection("count"))
        .aggregate(FlatEntitySummaryStats.class)
        .forEachRemaining(summaryStats::add);

    return getQlAggregatedData(firstLevelAggregation, dataPoints, summaryStats);
  }

  protected QLAggregatedData getQlAggregatedData(
      String firstLevelAggregation, List<QLDataPoint> dataPoints, List<FlatEntitySummaryStats> summaryStats) {
    Set<String> ids = summaryStats.stream().map(FlatEntitySummaryStats::getEntityId).collect(Collectors.toSet());
    NameResult nameResult = nameService.getNames(ids, firstLevelAggregation);
    summaryStats.forEach(getFlatEntitySummaryStatsConsumer(firstLevelAggregation, dataPoints, nameResult));
    return QLAggregatedData.builder().dataPoints(dataPoints).build();
  }

  @NotNull
  protected Consumer<FlatEntitySummaryStats> getFlatEntitySummaryStatsConsumer(
      String firstLevelAggregation, List<QLDataPoint> dataPoints, NameResult nameResult) {
    return flatEntitySummaryStats -> {
      QLDataPoint dataPoint = getDataPoint(flatEntitySummaryStats, firstLevelAggregation, nameResult);
      dataPoints.add(dataPoint);
    };
  }

  protected QLData getSingleDataPointData(Query query) {
    long count = query.count();
    return QLSinglePointData.builder()
        .dataPoint(QLDataPoint.builder()
                       .key(QLReference.builder().name(getEntityType()).id(getEntityType()).build())
                       .value(count)
                       .build())
        .build();
  }

  protected QLData getQLData(
      String accountId, List<? extends QLFilterType> filters, Class entityClass, List<String> groupByAsStringList) {
    Query query = populateFilters(accountId, filters, entityClass);
    if (isNotEmpty(groupByAsStringList)) {
      if (groupByAsStringList.size() == 1) {
        return getAggregatedData(groupByAsStringList, entityClass, query);
      } else if (groupByAsStringList.size() == 2) {
        return getStackedData(groupByAsStringList, entityClass, query);
      } else {
        throw new WingsException("Only 2 level aggregations supported right now");
      }
    } else {
      return getSingleDataPointData(query);
    }
  }

  @NotNull
  protected Query populateFilters(String accountId, List<? extends QLFilterType> filters, Class entityClass) {
    Query query = populateAccountFilter(accountId, entityClass);

    if (isNotEmpty(filters)) {
      filters.forEach(filter -> {
        if (filter.getDataType().equals(QLDataType.STRING)) {
          QLStringFilter stringFilter = ((QLStringFilterType) filter).getStringFilter();

          if (stringFilter == null) {
            throw new WingsException("Filter value is null for type:" + filter.getFilterType());
          }
          setStringFilter(query.field(getFilterFieldName(filter.getFilterType())), stringFilter);
        } else if (((QLFilterType) filter).getDataType().equals(QLDataType.NUMBER)) {
          QLNumberFilter numberFilter = ((QLNumberFilterType) filter).getNumberFilter();

          if (numberFilter == null) {
            throw new WingsException("Filter value is null for type:" + filter.getFilterType());
          }
          setNumberFilter(query.field(getFilterFieldName(filter.getFilterType())), numberFilter);
        }
      });
    }
    return query;
  }

  @NotNull
  protected Query populateAccountFilter(String accountId, Class entityClass) {
    Query query = wingsPersistence.createQuery(entityClass);
    query.filter(SettingAttributeKeys.accountId, accountId);
    return query;
  }

  protected String getFilterFieldName(String filterType) {
    throw new WingsException("Unknown filter type:" + filterType);
  }

  protected String getAggregationFieldName(String aggregation) {
    throw new WingsException("Unknown aggregation field:" + aggregation);
  }

  protected QLStackedData getStackedData(
      List<TwoLevelAggregatedData> aggregatedDataList, String firstLevelType, String secondLevelType) {
    QLStackedDataPoint prevStackedDataPoint = null;
    List<QLStackedDataPoint> stackedDataPointList = new ArrayList<>();

    Set<String> firstLevelIds = aggregatedDataList.stream()
                                    .map(aggregationData -> aggregationData.getFirstLevelInfo().getId())
                                    .collect(Collectors.toSet());

    Set<String> secondLevelIds = aggregatedDataList.stream()
                                     .map(aggregationData -> aggregationData.getSecondLevelInfo().getId())
                                     .collect(Collectors.toSet());

    NameResult firstLevelNameResult = nameService.getNames(firstLevelIds, firstLevelType);
    NameResult secondLevelNameResult = nameService.getNames(secondLevelIds, secondLevelType);

    for (TwoLevelAggregatedData aggregatedData : aggregatedDataList) {
      EntitySummary firstLevelInfo = aggregatedData.getFirstLevelInfo();
      EntitySummary secondLevelInfo = aggregatedData.getSecondLevelInfo();
      QLReference secondLevelRef =
          QLReference.builder()
              .type(secondLevelType)
              .name(getEntityName(secondLevelNameResult, secondLevelInfo.getId(), secondLevelType))
              .id(secondLevelInfo.getId())
              .build();
      QLDataPoint secondLevelDataPoint =
          QLDataPoint.builder().key(secondLevelRef).value(aggregatedData.getCount()).build();

      boolean sameAsPrevious =
          prevStackedDataPoint != null && prevStackedDataPoint.getKey().getId().equals(firstLevelInfo.getId());
      if (sameAsPrevious) {
        prevStackedDataPoint.getValues().add(secondLevelDataPoint);
      } else {
        QLReference firstLevelRef =
            QLReference.builder()
                .type(firstLevelType)
                .name(getEntityName(firstLevelNameResult, firstLevelInfo.getId(), firstLevelType))
                .id(firstLevelInfo.getId())
                .build();
        prevStackedDataPoint =
            QLStackedDataPoint.builder().key(firstLevelRef).values(Lists.newArrayList(secondLevelDataPoint)).build();
        stackedDataPointList.add(prevStackedDataPoint);
      }
    }

    return QLStackedData.builder().dataPoints(stackedDataPointList).build();
  }
}
