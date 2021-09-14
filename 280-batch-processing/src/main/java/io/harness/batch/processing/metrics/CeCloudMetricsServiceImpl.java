/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.metrics;

import static java.lang.String.format;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class CeCloudMetricsServiceImpl implements CeCloudMetricsService {
  private static final String TOTAL_CLOUD_COST_QUERY =
      "SELECT SUM(cost) AS COST  FROM `%s` WHERE cloudProvider='%s' and startTime>= '%s' and startTime<'%s'";
  @Autowired @Inject private BatchMainConfig batchMainConfig;
  @Autowired @Inject private BigQueryService bigQueryService;
  @Autowired @Inject private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  @Override
  public double getTotalCloudCost(String accountId, String cloudProviderType, Instant start, Instant end) {
    BillingDataPipelineConfig billingDataPipelineConfig = batchMainConfig.getBillingDataPipelineConfig();
    String projectId = billingDataPipelineConfig.getGcpProjectId();
    BillingDataPipelineRecord billingDataPipelineRecord = billingDataPipelineRecordDao.getByAccountId(accountId);
    if (billingDataPipelineRecord != null) {
      try {
        String dataSetId = billingDataPipelineRecord.getDataSetId();
        String tableName = format("%s.%s.%s", projectId, dataSetId, "preAggregated");

        TableResult result = null;
        String query = String.format(TOTAL_CLOUD_COST_QUERY, tableName, cloudProviderType, start, end);
        result = bigQueryService.get().query(QueryJobConfiguration.newBuilder(query).build());
        double totalCloudCost = 0;
        for (FieldValueList row : result.iterateAll()) {
          FieldValue value = row.get("COST");
          if (!value.isNull()) {
            totalCloudCost = value.getDoubleValue();
          }
        }
        return totalCloudCost;
      } catch (InterruptedException e) {
        log.error("Failed to get total cloud cost from PreAggregateBilling. ", e);
        Thread.currentThread().interrupt();
        return 0;
      }
    }
    return 0;
  }
}
