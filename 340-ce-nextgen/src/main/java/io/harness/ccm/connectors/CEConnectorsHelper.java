package io.harness.ccm.connectors;

import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CEConnectorsHelper {
  @Inject private BigQueryService bigQueryService;
  @Inject private CENextGenConfiguration configuration;

  public final String JOB_TYPE_CLOUDFUNCTION = "cloudfunction";
  public final String JOB_TYPE_BATCH = "batch";
  private final String BQ_DATA_SYNC_CHECK_TEMPLATE =
      "SELECT count(*) as count FROM `%s.CE_INTERNAL.connectorDataSyncStatus` "
      + "WHERE lastSuccessfullExecutionAt >= DATETIME_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY) "
      + "AND cloudProviderId = '%s' AND accountId = '%s' AND connectorId = '%s' AND jobType='%s';";

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  public boolean isDataSyncCheck(
      String accountIdentifier, String connectorIdentifier, ConnectorType connectorType, String jobType) {
    String cloudProvider = "";
    BigQuery bigquery = bigQueryService.get();
    String gcpProjectId = configuration.getGcpConfig().getGcpProjectId();
    switch (connectorType) {
      case CE_AWS:
        cloudProvider = "AWS";
        break;
      case CE_AZURE:
        cloudProvider = "AZURE";
        break;
      case GCP_CLOUD_COST:
        cloudProvider = "GCP";
        break;
      default:
        log.error("Unknown connector type: {}", connectorType);
        return false;
    }
    String query = String.format(
        BQ_DATA_SYNC_CHECK_TEMPLATE, gcpProjectId, cloudProvider, accountIdentifier, connectorIdentifier, jobType);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

    // Get the results.
    TableResult result;
    try {
      log.info("Running query: {}", query);
      result = bigquery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to check for data. {}", e);
      Thread.currentThread().interrupt();
      return false;
    }
    // Print all pages of the results.
    for (FieldValueList row : result.iterateAll()) {
      long count = row.get("count").getLongValue();
      if (count > 0) {
        return true;
      }
    }
    return false;
  }
}
