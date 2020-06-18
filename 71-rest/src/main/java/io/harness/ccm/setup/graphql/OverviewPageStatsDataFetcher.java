package io.harness.ccm.setup.graphql;

import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.countStringValueConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityCloudProviderConst;
import static io.harness.persistence.HQuery.excludeValidate;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.ccm.setup.graphql.QLCEOverviewStatsData.QLCEOverviewStatsDataBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OverviewPageStatsDataFetcher
    extends AbstractObjectDataFetcher<QLCEOverviewStatsData, QLNoOpQueryParameters> {
  @Inject HPersistence persistence;
  @Inject private BigQueryService bigQueryService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private TimeScaleDBService timeScaleDBService;

  private static final String DATA_SET_NAME_TEMPLATE = "BillingReport_%s";
  private static final String PRE_AGG_TABLE_NAME_VALUE = "preAggregated";
  private static final String appIdColumnName = "appid";
  private static final String clusterIdColumnName = "clusterid";
  private static final String countFieldName = "count";
  private static final String queryTemplate =
      "SELECT count(*) AS count FROM BILLING_DATA WHERE accountid = '%s' AND %s IS NOT NULL AND starttime >= '%s'";
  private static final String bigQueryTemplate =
      "SELECT count(*) AS count, cloudProvider FROM `%s.%s.%s` GROUP BY cloudProvider";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEOverviewStatsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    boolean isAWSConnectorPresent = false;
    boolean isGCPConnectorPresent = false;
    boolean isApplicationDataPresent = false;
    boolean isClusterDataPresent = false;
    List<SettingAttribute> ceConnectorsList = getCEConnectors(accountId);
    QLCEOverviewStatsDataBuilder overviewStatsDataBuilder = QLCEOverviewStatsData.builder();

    // Cloud Connector Present
    for (SettingAttribute settingAttribute : ceConnectorsList) {
      if (settingAttribute.getValue().getType().equals(SettingVariableTypes.CE_AWS.toString())) {
        isAWSConnectorPresent = true;
      }
      if (settingAttribute.getValue().getType().equals(SettingVariableTypes.CE_GCP.toString())) {
        isGCPConnectorPresent = true;
      }
    }
    overviewStatsDataBuilder.cloudConnectorsPresent(isAWSConnectorPresent || isGCPConnectorPresent);

    // Cluster, Application Data Present
    Instant sevenDaysPriorInstant =
        Instant.ofEpochMilli(Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli() - TimeUnit.DAYS.toMillis(7));
    String applicationQuery = String.format(queryTemplate, accountId, appIdColumnName, sevenDaysPriorInstant);
    String clusterQuery = String.format(queryTemplate, accountId, clusterIdColumnName, sevenDaysPriorInstant);

    if (getCount(applicationQuery, accountId) != 0) {
      isApplicationDataPresent = true;
    }

    if (getCount(clusterQuery, accountId) != 0) {
      isClusterDataPresent = true;
    }
    overviewStatsDataBuilder.clusterDataPresent(isClusterDataPresent).applicationDataPresent(isApplicationDataPresent);

    // AWS, GCP Data Present
    String dataSetId = String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountId));
    TableId tableId = TableId.of(dataSetId, PRE_AGG_TABLE_NAME_VALUE);
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
    String projectId = ceSetUpConfig.getGcpProjectId();
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration
            .newBuilder(String.format(bigQueryTemplate, projectId, dataSetId, PRE_AGG_TABLE_NAME_VALUE))
            .build();

    try {
      BigQuery bigQuery = bigQueryService.get();
      Table table = getTableFromBQ(tableId, bigQuery);
      if (null != table) {
        TableResult result = bigQuery.query(queryConfig);
        for (FieldValueList row : result.iterateAll()) {
          modifyOverviewStatsBuilder(row, overviewStatsDataBuilder);
        }
      } else {
        overviewStatsDataBuilder.awsConnectorsPresent(Boolean.FALSE).gcpConnectorsPresent(Boolean.FALSE);
      }
    } catch (InterruptedException e) {
      logger.error("Failed to get OverviewPageStatsDataFetcher {}", e);
      Thread.currentThread().interrupt();
    }

    return overviewStatsDataBuilder.ceEnabledClusterPresent(getCEEnabledCloudProvider(accountId)).build();
  }

  void modifyOverviewStatsBuilder(FieldValueList row, QLCEOverviewStatsDataBuilder overviewStatsDataBuilder) {
    String cloudProvider = row.get(entityCloudProviderConst).getStringValue();
    switch (cloudProvider) {
      case "AWS":
        overviewStatsDataBuilder.awsConnectorsPresent(row.get(countStringValueConstant).getDoubleValue() > 0);
        break;
      case "GCP":
        overviewStatsDataBuilder.gcpConnectorsPresent(row.get(countStringValueConstant).getDoubleValue() > 0);
        break;
      default:
        break;
    }
  }

  private Table getTableFromBQ(TableId tableId, BigQuery bigQuery) {
    return bigQuery.getTable(tableId);
  }

  protected Integer getCount(String query, String accountId) {
    int count = 0;
    if (timeScaleDBService.isValid()) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(query);
        while (resultSet != null && resultSet.next()) {
          count = resultSet.getInt(countFieldName);
        }
      } catch (SQLException e) {
        logger.warn("Failed to execute query in OverviewPageStatsDataFetcher, query=[{}], accountId=[{}], {}", query,
            accountId, e);
      } finally {
        DBUtils.close(resultSet);
      }
    } else {
      throw new InvalidRequestException("Cannot process request in OverviewPageStatsDataFetcher");
    }
    return count;
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  protected List<SettingAttribute> getCEConnectors(String accountId) {
    return persistence.createQuery(SettingAttribute.class)
        .field(SettingAttributeKeys.accountId)
        .equal(accountId)
        .field(SettingAttributeKeys.category)
        .equal(SettingCategory.CE_CONNECTOR.toString())
        .asList();
  }

  protected boolean getCEEnabledCloudProvider(String accountId) {
    return null
        != persistence.createQuery(SettingAttribute.class, excludeValidate)
               .field(SettingAttributeKeys.accountId)
               .equal(accountId)
               .field(SettingAttributeKeys.category)
               .equal(SettingCategory.CLOUD_PROVIDER.toString())
               .field(SettingAttributeKeys.isCEEnabled)
               .equal(true)
               .get();
  }
}
