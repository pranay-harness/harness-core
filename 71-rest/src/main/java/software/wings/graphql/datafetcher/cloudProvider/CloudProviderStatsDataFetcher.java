package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLFilterType;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilterType;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CloudProviderStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLCloudProviderFilter,
    QLCloudProviderAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLCloudProviderFilter> filters,
      List<QLCloudProviderAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    final Class entityClass = SettingAttribute.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  @NotNull
  @Override
  protected Query populateFilters(String accountId, List<? extends QLFilterType> filters, Class entityClass) {
    Query query = super.populateFilters(accountId, filters, entityClass);
    query.filter(SettingAttributeKeys.category, SettingCategory.CLOUD_PROVIDER);
    return query;
  }

  protected QLAggregatedData getAggregatedData(List<String> groupBy, Class entityClass, Query query) {
    String firstLevelAggregation = groupBy.get(0);
    String entityIdColumn = getAggregationFieldName(firstLevelAggregation);

    List<Group> idField = Group.id(grouping(entityIdColumn));

    final String[] entityIdFields = entityIdColumn.split("\\.");
    if (entityIdFields.length > 1) {
      idField = Group.id(grouping(entityIdFields[entityIdFields.length - 1], entityIdColumn));
    }
    List<QLDataPoint> dataPoints = new ArrayList<>();

    final AggregationPipeline aggregationPipeline =
        wingsPersistence.getDatastore(entityClass)
            .createAggregation(entityClass)
            .match(query)
            .group(idField, grouping("count", new Accumulator("$sum", 1)))
            .project(projection("_id").suppress(),
                projection("entityId", "_id." + entityIdFields[entityIdFields.length - 1]),
                projection("entityName", "_id." + entityIdFields[entityIdFields.length - 1]), projection("count"));
    aggregationPipeline.aggregate(FlatEntitySummaryStats.class).forEachRemaining(flatEntitySummaryStats -> {
      QLDataPoint dataPoint = getDataPoint(flatEntitySummaryStats, firstLevelAggregation);
      dataPoints.add(dataPoint);
    });

    return QLAggregatedData.builder().dataPoints(dataPoints).build();
  }

  protected String getFilterFieldName(String filterType) {
    QLCloudProviderFilterType qlFilterType = QLCloudProviderFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Type:
        return "value.type";
      case CloudProvider:
        return "_id";
      case CreatedAt:
        return "createdAt";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLCloudProviderAggregation qlEnvironmentAggregation = QLCloudProviderAggregation.valueOf(aggregation);
    switch (qlEnvironmentAggregation) {
      case Type:
        return "value.type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  protected String getAggregationNameField(String aggregation) {
    QLCloudProviderAggregation qlEnvironmentAggregation = QLCloudProviderAggregation.valueOf(aggregation);
    switch (qlEnvironmentAggregation) {
      case Type:
        return "type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }
}
