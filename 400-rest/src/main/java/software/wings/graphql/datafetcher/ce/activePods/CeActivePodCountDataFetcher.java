/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.activePods;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.datafetcher.ce.activePods.CeActivePodCountQueryMetadata.CeActivePodCountMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLPodCountDataPoint;
import software.wings.graphql.schema.type.aggregation.QLPodCountDataPoint.QLPodCountDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLPodCountTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLPodCountTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CeActivePodCountDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, filters, sort);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingTrendStatsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while billing data", e);
    }
  }

  protected QLPodCountTimeSeriesData getData(
      @NotNull String accountId, List<QLBillingDataFilter> filters, List<QLBillingSortCriteria> sort) {
    CeActivePodCountQueryMetadata queryData;
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;

    queryData = billingDataQueryBuilder.formPodCountQuery(accountId, filters, sort);
    log.info("CeActivePodCountDataFetcher query: {}", queryData.getQuery());

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        return generateStackedTimeSeriesData(queryData, resultSet);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          log.error(
              "Failed to execute query in CeActivePodCountDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          log.warn("Failed to execute query in CeActivePodCountDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  protected QLPodCountTimeSeriesData generateStackedTimeSeriesData(
      CeActivePodCountQueryMetadata queryData, ResultSet resultSet) throws SQLException {
    Map<Long, List<QLPodCountDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();
    long time = 0L;
    while (resultSet != null && resultSet.next()) {
      QLPodCountDataPointBuilder dataPointBuilder = QLPodCountDataPoint.builder();
      for (CeActivePodCountMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            dataPointBuilder.value(resultSet.getDouble(field.getFieldName()));
            break;
          case STRING:
            switch (field) {
              case INSTANCEID:
                String nodeId = resultSet.getString(field.getFieldName());
                dataPointBuilder.key(QLReference.builder().id(nodeId).name(nodeId).build());
                break;
              case CLUSTERID:
              default:
            }
            break;
          case TIMESTAMP:
            time = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      checkDataPointIsValidAndInsert(dataPointBuilder.build(), qlTimeDataPointMap, time);
    }

    List<QLPodCountTimeSeriesDataPoint> data = new ArrayList<>();
    qlTimeDataPointMap.keySet().forEach(
        key -> data.add(QLPodCountTimeSeriesDataPoint.builder().time(key).values(qlTimeDataPointMap.get(key)).build()));

    return QLPodCountTimeSeriesData.builder().data(data).build();
  }

  private void checkDataPointIsValidAndInsert(
      QLPodCountDataPoint dataPoint, Map<Long, List<QLPodCountDataPoint>> qlTimeDataPointMap, long time) {
    qlTimeDataPointMap.computeIfAbsent(time, k -> new ArrayList<>());
    qlTimeDataPointMap.get(time).add(dataPoint);
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregations, List<QLBillingSortCriteria> sort, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return true;
  }
}
