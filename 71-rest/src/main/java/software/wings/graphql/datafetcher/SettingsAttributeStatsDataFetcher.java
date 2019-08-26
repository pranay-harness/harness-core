package software.wings.graphql.datafetcher;

import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;

import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.FlatEntitySummaryStats;

import java.util.ArrayList;
import java.util.List;

public abstract class SettingsAttributeStatsDataFetcher<A, F, G, T, TG, S>
    extends RealTimeStatsDataFetcher<A, F, G, T, TG, S> {
  protected QLAggregatedData getAggregatedData(List<String> groupBy, Class entityClass, Query query) {
    String firstLevelAggregation = groupBy.get(0);
    String entityIdColumn = getAggregationFieldName(firstLevelAggregation);

    List<Group> idField = Group.id(grouping(entityIdColumn));

    final String[] entityIdFields = entityIdColumn.split("\\.");
    if (entityIdFields.length > 1) {
      idField = Group.id(grouping(entityIdFields[entityIdFields.length - 1], entityIdColumn));
    }
    List<QLDataPoint> dataPoints = new ArrayList<>();
    List<FlatEntitySummaryStats> summaryStats = new ArrayList<>();
    final AggregationPipeline aggregationPipeline =
        wingsPersistence.getDatastore(entityClass)
            .createAggregation(entityClass)
            .match(query)
            .group(idField, grouping("count", new Accumulator("$sum", 1)))
            .project(projection("_id").suppress(),
                projection("entityId", "_id." + entityIdFields[entityIdFields.length - 1]),
                projection("entityName", "_id." + entityIdFields[entityIdFields.length - 1]), projection("count"));
    aggregationPipeline.aggregate(FlatEntitySummaryStats.class).forEachRemaining(summaryStats::add);
    return getQlAggregatedData(firstLevelAggregation, dataPoints, summaryStats);
  }
}
