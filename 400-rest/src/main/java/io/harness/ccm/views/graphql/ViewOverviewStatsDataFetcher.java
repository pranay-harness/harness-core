/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.ccm.views.graphql.QLViewOverviewStatsData.QLViewOverviewStatsDataBuilder;
import io.harness.persistence.HPersistence;

import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class ViewOverviewStatsDataFetcher
    extends AbstractObjectDataFetcher<QLViewOverviewStatsData, QLNoOpQueryParameters> {
  @Inject HPersistence persistence;
  @Inject private BigQueryService bigQueryService;
  @Inject private MainConfiguration mainConfiguration;

  private static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  private static final String UNIFIED_TABLE_NAME_VALUE = "unifiedTable";
  public static final String countStringValueConstant = "count";
  private static final String bigQueryTemplate = "SELECT count(*) AS count FROM `%s.%s.%s`";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLViewOverviewStatsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    QLViewOverviewStatsDataBuilder viewOverviewStatsDataBuilder = QLViewOverviewStatsData.builder();
    viewOverviewStatsDataBuilder.isAwsOrGcpOrClusterConfigured(
        getCEConnectors(accountId) || getCEEnabledCloudProvider(accountId));

    String dataSetId = String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountId));
    TableId tableId = TableId.of(dataSetId, UNIFIED_TABLE_NAME_VALUE);
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
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
      log.error("Failed to get ViewOverviewStatsDataFetcher {}", e);
      Thread.currentThread().interrupt();
    }

    return viewOverviewStatsDataBuilder.build();
  }

  private Table getTableFromBQ(TableId tableId, BigQuery bigQuery) {
    return bigQuery.getTable(tableId);
  }

  protected boolean getCEEnabledCloudProvider(String accountId) {
    return null
        != persistence.createQuery(SettingAttribute.class, excludeValidate)
               .field(SettingAttributeKeys.accountId)
               .equal(accountId)
               .field(SettingAttributeKeys.category)
               .equal(SettingAttribute.SettingCategory.CLOUD_PROVIDER.toString())
               .field(SettingAttributeKeys.isCEEnabled)
               .equal(true)
               .get();
  }

  protected boolean getCEConnectors(String accountId) {
    return null
        != persistence.createQuery(SettingAttribute.class)
               .field(SettingAttributeKeys.accountId)
               .equal(accountId)
               .field(SettingAttributeKeys.category)
               .equal(SettingAttribute.SettingCategory.CE_CONNECTOR.toString())
               .get();
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }
}
