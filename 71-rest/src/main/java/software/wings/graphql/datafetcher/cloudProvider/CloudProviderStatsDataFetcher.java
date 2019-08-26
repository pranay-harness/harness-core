package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.SettingsAttributeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CloudProviderStatsDataFetcher extends SettingsAttributeStatsDataFetcher<QLNoOpAggregateFunction,
    QLCloudProviderFilter, QLCloudProviderAggregation, QLTimeSeriesAggregation, QLTagAggregation, QLNoOpSortCriteria> {
  @Inject CloudProviderQueryHelper cloudProviderQueryHelper;
  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction,
      List<QLCloudProviderFilter> filters, List<QLCloudProviderAggregation> groupBy,
      QLTimeSeriesAggregation groupByTime, List<QLTagAggregation> tagAggregationList,
      List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = SettingAttribute.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  @Override
  @NotNull
  protected Query populateFilters(
      WingsPersistence wingsPersistence, String accountId, List<QLCloudProviderFilter> filters, Class entityClass) {
    Query query = super.populateFilters(wingsPersistence, accountId, filters, entityClass);
    query.filter(SettingAttributeKeys.category, SettingCategory.CLOUD_PROVIDER);
    return query;
  }

  protected String getAggregationFieldName(String aggregation) {
    QLCloudProviderAggregation cloudProviderAggregation = QLCloudProviderAggregation.valueOf(aggregation);
    switch (cloudProviderAggregation) {
      case Type:
        return "value.type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(List<QLCloudProviderFilter> filters, Query query) {
    cloudProviderQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.cloudProvider;
  }
}
