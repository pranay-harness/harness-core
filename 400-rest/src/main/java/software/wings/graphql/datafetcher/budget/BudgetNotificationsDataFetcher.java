/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.budget;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.budget.BudgetService;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetNotifications;
import software.wings.graphql.schema.type.aggregation.budget.QLBudgetNotificationsData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BudgetNotificationsDataFetcher extends AbstractStatsDataFetcher<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject BudgetService budgetService;
  @Inject BudgetTimescaleQueryHelper queryHelper;
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, QLCCMAggregationFunction aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort,
      DataFetchingEnvironment dataFetchingEnvironment) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        return QLBudgetNotificationsData.builder()
            .data(QLBudgetNotifications.builder().count(getAlertCount(accountId, filters)).build())
            .build();
      } else {
        throw new InvalidRequestException("Cannot process request in BudgetNotificationsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching budget alerts count {}", e);
    }
  }

  private long getAlertCount(String accountId, List<QLBillingDataFilter> filters) {
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    BillingDataQueryMetadata queryData = queryHelper.formBudgetAlertsCountQuery(accountId, filters);
    log.info("BudgetNotificationsDataFetcher query to get alert count!! {}", queryData.getQuery());
    long alertCount = 0L;
    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        while (resultSet.next()) {
          for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
            if (field == BillingDataQueryMetadata.BillingDataMetaDataFields.COUNT) {
              alertCount = resultSet.getLong(field.getFieldName());
            }
          }
        }
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          log.error(
              "Failed to execute query in BudgetNotificationsDataFetcher to get budget alert count!, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          log.warn(
              "Failed to execute query in BudgetNotificationsDataFetcher to get budget alert count!, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return alertCount;
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
