package io.harness.ccm.setup.graphql;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
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
  @Inject private TimeScaleDBService timeScaleDBService;

  private static final String appIdColumnName = "appid";
  private static final String clusterIdColumnName = "clusterid";
  private static final String countFieldName = "count";
  private static final String queryTemplate =
      "SELECT count(*) AS count FROM BILLING_DATA WHERE accountid = '%s' AND %s IS NOT NULL AND starttime >= '%s'";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEOverviewStatsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    boolean isAWSConnectorPresent = false;
    boolean isGCPConnectorPresent = false;
    boolean isApplicationDataPresent = false;
    boolean isClusterDataPresent = false;
    List<SettingAttribute> ceConnectorsList = getCEConnectors(accountId);

    for (SettingAttribute settingAttribute : ceConnectorsList) {
      if (settingAttribute.getValue().getType().equals(SettingVariableTypes.CE_AWS.toString())) {
        isAWSConnectorPresent = true;
      }
      if (settingAttribute.getValue().getType().equals(SettingVariableTypes.CE_GCP.toString())) {
        isGCPConnectorPresent = true;
      }
    }

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

    return QLCEOverviewStatsData.builder()
        .cloudConnectorsPresent(isAWSConnectorPresent || isGCPConnectorPresent)
        .awsConnectorsPresent(isAWSConnectorPresent)
        .gcpConnectorsPresent(isGCPConnectorPresent)
        .applicationDataPresent(isApplicationDataPresent)
        .clusterDataPresent(isClusterDataPresent)
        .build();
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

  protected List<SettingAttribute> getCEConnectors(String accountId) {
    return persistence.createQuery(SettingAttribute.class)
        .field(SettingAttributeKeys.accountId)
        .equal(accountId)
        .field(SettingAttributeKeys.category)
        .equal(SettingCategory.CE_CONNECTOR.toString())
        .asList();
  }
}
