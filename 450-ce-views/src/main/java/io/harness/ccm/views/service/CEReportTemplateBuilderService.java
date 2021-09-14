/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
