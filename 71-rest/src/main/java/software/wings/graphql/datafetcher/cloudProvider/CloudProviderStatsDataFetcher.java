package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.SettingsAttributeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLFilterType;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderAggregation;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilterType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CloudProviderStatsDataFetcher extends SettingsAttributeStatsDataFetcher<QLAggregateFunction,
    QLCloudProviderFilter, QLCloudProviderAggregation, QLTimeSeriesAggregation> {
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
    QLCloudProviderAggregation cloudProviderAggregation = QLCloudProviderAggregation.valueOf(aggregation);
    switch (cloudProviderAggregation) {
      case Type:
        return "value.type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  protected String getAggregationNameField(String aggregation) {
    QLCloudProviderAggregation cloudProviderAggregation = QLCloudProviderAggregation.valueOf(aggregation);
    switch (cloudProviderAggregation) {
      case Type:
        return "type";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.cloudProvider;
  }
}
