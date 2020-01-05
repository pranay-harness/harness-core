package software.wings.graphql.datafetcher.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.SettingsAttributeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorAggregation;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilter;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorTypeAggregation;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectorStatsDataFetcher extends SettingsAttributeStatsDataFetcher<QLNoOpAggregateFunction,
    QLConnectorFilter, QLConnectorAggregation, QLNoOpSortCriteria> {
  @Inject ConnectorQueryHelper connectorQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLConnectorFilter> filters,
      List<QLConnectorAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = SettingAttribute.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream()
                        .filter(g -> g != null && g.getTypeAggregation() != null)
                        .map(g -> g.getTypeAggregation().name())
                        .collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  @NotNull
  @Override
  public Query populateFilters(DataFetcherUtils utils, WingsPersistence wingsPersistence, String accountId,
      List<QLConnectorFilter> filters, Class entityClass) {
    Query query = super.populateFilters(utils, wingsPersistence, accountId, filters, entityClass);
    query.field(SettingAttributeKeys.category)
        .in(Lists.newArrayList(SettingCategory.CONNECTOR, SettingCategory.HELM_REPO));
    return query;
  }

  @Override
  public void populateFilters(String accountId, List<QLConnectorFilter> filters, Query query) {
    connectorQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getAggregationFieldName(String aggregation) {
    QLConnectorTypeAggregation connectorAggregation = QLConnectorTypeAggregation.valueOf(aggregation);
    switch (connectorAggregation) {
      case Type:
        return "value.type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.connector;
  }
}
