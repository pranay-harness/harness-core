package io.harness.ccm.views.service;

import com.google.cloud.bigquery.BigQuery;
import java.util.Map;

public interface CEReportTemplateBuilderService {
  // For ad-hoc reports
  Map<String, String> getTemplatePlaceholders(
      String accountId, String viewId, BigQuery bigQuery, String cloudProviderTableName);

  // For batch-job scheduled reports
  Map<String, String> getTemplatePlaceholders(
      String accountId, String viewId, String reportId, BigQuery bigQuery, String cloudProviderTableName);
}
