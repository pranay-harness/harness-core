package io.harness.batch.processing.billing.timeseries.service.impl;

import io.harness.batch.processing.billing.timeseries.service.support.BillingDataTableNameProvider;
import io.harness.batch.processing.ccm.BatchJobType;
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
      "SELECT SUM(BILLINGAMOUNT) AS COST, SUM(CPUBILLINGAMOUNT) AS CPUCOST, SUM(MEMORYBILLINGAMOUNT) AS MEMORYCOST, SUM(SYSTEMCOST) AS SYSTEMCOST, SUM(CPUSYSTEMCOST) AS CPUSYSTEMCOST, SUM(MEMORYSYSTEMCOST) AS MEMORYSYSTEMCOST,  ACCOUNTID, CLUSTERID, INSTANCETYPE FROM %s WHERE ACCOUNTID = '%s' AND CLUSTERID IS NOT NULL AND INSTANCETYPE IN ('K8S_POD', 'K8S_NODE', 'ECS_CONTAINER_INSTANCE', 'ECS_TASK_EC2') AND STARTTIME >= '%s' AND STARTTIME < '%s' GROUP BY ACCOUNTID, CLUSTERID, INSTANCETYPE";

  static final String GET_COMMON_FIELDS =
      "SELECT BILLINGACCOUNTID, ACCOUNTID, CLUSTERNAME, SETTINGID, REGION, CLOUDPROVIDER, CLUSTERTYPE, WORKLOADTYPE FROM %s WHERE ACCOUNTID = '%s' AND CLUSTERID = '%s' AND INSTANCETYPE IN ('K8S_POD', 'K8S_NODE', 'ECS_CONTAINER_INSTANCE', 'ECS_TASK_EC2') AND STARTTIME >= '%s' AND STARTTIME < '%s' LIMIT 1";

  public List<UnallocatedCostData> getUnallocatedCostData(
      String accountId, long startDate, long endDate, BatchJobType batchJobType) {
    ResultSet resultSet = null;
    List<UnallocatedCostData> unallocatedCostDataList = new ArrayList<>();

    String query = String.format(GET_UNALLOCATED_COST_DATA, BillingDataTableNameProvider.getTableName(batchJobType),
        accountId, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        unallocatedCostDataList.add(UnallocatedCostData.builder()
                                        .accountId(resultSet.getString("ACCOUNTID"))
                                        .clusterId(resultSet.getString("CLUSTERID"))
                                        .instanceType(resultSet.getString("INSTANCETYPE"))
                                        .cost(resultSet.getDouble("COST"))
                                        .cpuCost(resultSet.getDouble("CPUCOST"))
                                        .memoryCost(resultSet.getDouble("MEMORYCOST"))
                                        .systemCost(resultSet.getDouble("SYSTEMCOST"))
                                        .cpuSystemCost(resultSet.getDouble("CPUSYSTEMCOST"))
                                        .memorySystemCost(resultSet.getDouble("MEMORYSYSTEMCOST"))
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

  public ClusterCostData getCommonFields(
      String accountId, String clusterId, long startDate, long endDate, BatchJobType batchJobType) {
    ResultSet resultSet = null;
    ClusterCostData clusterCostData = ClusterCostData.builder().build();

    String query = String.format(GET_COMMON_FIELDS, BillingDataTableNameProvider.getTableName(batchJobType), accountId,
        clusterId, Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
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
