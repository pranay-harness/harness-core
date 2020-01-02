package io.harness.batch.processing.billing.timeseries.service.impl;

import io.harness.batch.processing.ccm.ClusterCostData;
import io.harness.batch.processing.ccm.UnallocatedCostData;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class UnallocatedBillingDataServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;

  static final String GET_UNALLOCATED_COST_DATA =
      "SELECT SUM(BILLINGAMOUNT) AS COST, SUM(CPUBILLINGAMOUNT) AS CPUCOST, SUM(MEMORYBILLINGAMOUNT) AS MEMORYCOST, CLUSTERID, INSTANCETYPE FROM BILLING_DATA WHERE CLUSTERID IS NOT NULL AND INSTANCETYPE IN ('K8S_POD', 'K8S_NODE', 'ECS_CONTAINER_INSTANCE', 'ECS_TASK_EC2') AND STARTTIME >= '%s' AND ENDTIME <= '%s' GROUP BY CLUSTERID, INSTANCETYPE";

  static final String GET_COMMON_FIELDS =
      "SELECT BILLINGACCOUNTID, ACCOUNTID, CLUSTERNAME, SETTINGID, REGION, CLOUDPROVIDER, CLUSTERTYPE, WORKLOADTYPE FROM BILLING_DATA WHERE CLUSTERID = '%s' AND INSTANCETYPE IN ('K8S_POD', 'K8S_NODE', 'ECS_CONTAINER_INSTANCE', 'ECS_TASK_EC2') AND STARTTIME >= '%s' AND ENDTIME <= '%s' LIMIT 1";

  public List<UnallocatedCostData> getUnallocatedCostData(long startDate, long endDate) {
    ResultSet resultSet = null;
    List<UnallocatedCostData> unallocatedCostDataList = new ArrayList<>();

    String query =
        String.format(GET_UNALLOCATED_COST_DATA, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        unallocatedCostDataList.add(UnallocatedCostData.builder()
                                        .clusterId(resultSet.getString("CLUSTERID"))
                                        .instanceType(resultSet.getString("INSTANCETYPE"))
                                        .cost(resultSet.getDouble("COST"))
                                        .cpuCost(resultSet.getDouble("CPUCOST"))
                                        .memoryCost(resultSet.getDouble("MEMORYCOST"))
                                        .startTime(startDate)
                                        .endTime(endDate)
                                        .build());
      }
      return unallocatedCostDataList;
    } catch (SQLException e) {
      logger.error("Error while fetching Unallocated Cost Data List : exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return Collections.emptyList();
  }

  public ClusterCostData getCommonFields(String clusterId, long startDate, long endDate) {
    ResultSet resultSet = null;
    ClusterCostData clusterCostData = ClusterCostData.builder().build();

    String query =
        String.format(GET_COMMON_FIELDS, clusterId, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        clusterCostData = clusterCostData.toBuilder()
                              .billingAccountId(resultSet.getString("BILLINGACCOUNTID"))
                              .accountId(resultSet.getString("ACCOUNTID"))
                              .clusterName(resultSet.getString("CLUSTERNAME"))
                              .settingId(resultSet.getString("SETTINGID"))
                              .region(resultSet.getString("REGION"))
                              .cloudProvider(resultSet.getString("CLOUDPROVIDER"))
                              .clusterType(resultSet.getString("CLUSTERTYPE"))
                              .workloadType(resultSet.getString("WORKLOADTYPE"))
                              .build();
      }
      return clusterCostData;
    } catch (SQLException e) {
      logger.error("Error while fetching Common Fields : exception", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return clusterCostData;
  }
}
