/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.perspectives;

import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveOverviewStatsData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveOverviewStatsData.PerspectiveOverviewStatsDataBuilder;
import io.harness.persistence.HPersistence;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerspectiveOverviewStatsHelper {
  @Inject HPersistence persistence;
  @Inject private BigQueryService bigQueryService;
  @Inject BigQueryHelper bigQueryHelper;

  private static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  private static final String UNIFIED_TABLE_NAME_VALUE = "unifiedTable";
  public static final String countStringValueConstant = "count";
  private static final String bigQueryTemplate = "SELECT count(*) AS count FROM `%s.%s.%s`";

  public PerspectiveOverviewStatsData fetch(String accountId) {
    PerspectiveOverviewStatsDataBuilder viewOverviewStatsDataBuilder = PerspectiveOverviewStatsData.builder();
    // Todo: Add logic for isAwsOrGcpOrClusterConfigured

    String dataSetId = String.format(DATA_SET_NAME_TEMPLATE, bigQueryHelper.modifyStringToComplyRegex(accountId));
    TableId tableId = TableId.of(dataSetId, UNIFIED_TABLE_NAME_VALUE);
    String projectId = bigQueryHelper.getGcpProjectId();
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration
            .newBuilder(String.format(bigQueryTemplate, projectId, dataSetId, UNIFIED_TABLE_NAME_VALUE))
            .build();

    try {
      BigQuery bigQuery = bigQueryService.get();
      Table table = getTableFromBQ(tableId, bigQuery);
      if (null != table) {
        TableResult result = bigQuery.query(queryConfig);
        for (FieldValueList row : result.iterateAll()) {
          viewOverviewStatsDataBuilder.unifiedTableDataPresent(row.get(countStringValueConstant).getDoubleValue() > 0);
        }
      } else {
        viewOverviewStatsDataBuilder.unifiedTableDataPresent(Boolean.FALSE);
      }
    } catch (InterruptedException e) {
      log.error("Failed to get Perspective overview stats {}", e);
      Thread.currentThread().interrupt();
    }

    return viewOverviewStatsDataBuilder.build();
  }

  private Table getTableFromBQ(TableId tableId, BigQuery bigQuery) {
    return bigQuery.getTable(tableId);
  }
}
