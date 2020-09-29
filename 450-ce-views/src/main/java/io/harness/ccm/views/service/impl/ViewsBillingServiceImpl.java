package io.harness.ccm.views.service.impl;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.ccm.views.graphql.ViewsQueryMetadata;
import io.harness.ccm.views.service.ViewsBillingService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ViewsBillingServiceImpl implements ViewsBillingService {
  @Inject ViewsQueryBuilder viewsQueryBuilder;
  public static final String nullStringValueConstant = "Others";

  @Override
  public List<String> getFilterValueStats(
      BigQuery bigQuery, List<QLCEViewFilter> filters, String cloudProviderTableName, Integer limit, Integer offset) {
    ViewsQueryMetadata viewsQueryMetadata =
        viewsQueryBuilder.getFilterValuesQuery(filters, cloudProviderTableName, limit, offset);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(viewsQueryMetadata.getQuery().toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to getViewFilterValueStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToFilterValuesData(result, viewsQueryMetadata.getFields());
  }

  List<String> convertToFilterValuesData(TableResult result, List<QLCEViewFieldInput> viewFieldList) {
    List<String> filterValues = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      for (QLCEViewFieldInput field : viewFieldList) {
        filterValues.add(fetchStringValue(row, field));
      }
    }
    return filterValues;
  }

  private String fetchStringValue(FieldValueList row, QLCEViewFieldInput field) {
    Object value = row.get(viewsQueryBuilder.getAliasFromField(field)).getValue();
    if (value != null) {
      return value.toString();
    }
    return nullStringValueConstant;
  }
}
