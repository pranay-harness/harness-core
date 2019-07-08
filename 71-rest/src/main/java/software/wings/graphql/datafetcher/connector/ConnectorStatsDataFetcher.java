package software.wings.graphql.datafetcher.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.SettingsAttributeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLFilterType;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorAggregation;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilter;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilterType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectorStatsDataFetcher extends SettingsAttributeStatsDataFetcher<QLAggregateFunction, QLConnectorFilter,
    QLConnectorAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLConnectorFilter> filters,
      List<QLConnectorAggregation> groupBy, QLTimeSeriesAggregation groupByTime,
      List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = SettingAttribute.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  @NotNull
  @Override
  protected Query populateFilters(
      WingsPersistence wingsPersistence, String accountId, List<? extends QLFilterType> filters, Class entityClass) {
    Query query = super.populateFilters(wingsPersistence, accountId, filters, entityClass);
    query.filter(SettingAttributeKeys.category, SettingCategory.CONNECTOR);
    return query;
  }

  protected String getFilterFieldName(String filterType) {
    QLConnectorFilterType qlFilterType = QLConnectorFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Type:
        return "value.type";
      case Connector:
        return "_id";
      case CreatedAt:
        return "createdAt";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLConnectorAggregation connectorAggregation = QLConnectorAggregation.valueOf(aggregation);
    switch (connectorAggregation) {
      case Type:
        return "value.type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  protected String getAggregationNameField(String aggregation) {
    QLConnectorAggregation connectorAggregation = QLConnectorAggregation.valueOf(aggregation);
    switch (connectorAggregation) {
      case Type:
        return "type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.connector;
  }
}
