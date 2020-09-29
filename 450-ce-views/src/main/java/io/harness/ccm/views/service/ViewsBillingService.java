package io.harness.ccm.views.service;

import com.google.cloud.bigquery.BigQuery;

import io.harness.ccm.views.graphql.QLCEViewFilter;

import java.util.List;

public interface ViewsBillingService {
  List<String> getFilterValueStats(
      BigQuery bigQuery, List<QLCEViewFilter> filters, String cloudProviderTableName, Integer limit, Integer offset);
}
