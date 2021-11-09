package io.harness.ccm.connectors;

import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static io.harness.ccm.commons.utils.BigQueryHelper.DATA_SET_NAME_TEMPLATE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEGcpConnectorValidator extends io.harness.ccm.connectors.AbstractCEConnectorValidator {
  public static final String GCP_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  public static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";
  @Autowired
  private  BigQueryService bigQueryService;
  @Inject
  
  CENextGenConfiguration configuration;

  private static final String BQ_PRE_AGG_TABLE_DATACHECK_TEMPLATE =
          "SELECT count(*) as count FROM `%s.preAggregated` WHERE DATE(startTime) "
                  + ">= DATE_SUB(@run_date , INTERVAL 3 DAY) AND cloudProvider = \"%s\";%n";

  public ConnectorValidationResult validate(
      ConnectorResponseDTO connectorResponseDTO, String accountIdentifier) {
    final GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
        (GcpCloudCostConnectorDTO) connectorResponseDTO.getConnector().getConnectorConfig();
    String projectIdentifier = connectorResponseDTO.getConnector().getProjectIdentifier();
    String orgIdentifier = connectorResponseDTO.getConnector().getOrgIdentifier();
    String connectorIdentifier = connectorResponseDTO.getConnector().getIdentifier();
    String projectId = gcpCloudCostConnectorDTO.getProjectId();
    String datasetId = gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId();
    String impersonatedServiceAccount = gcpCloudCostConnectorDTO.getServiceAccountEmail();
    try {
      ConnectorValidationResult connectorValidationResult =
          validateAccessToBillingReport(projectId, datasetId, impersonatedServiceAccount);
      if (connectorValidationResult != null) {
        return connectorValidationResult;
      } else {
        String dataSetName =
                String.format(DATA_SET_NAME_TEMPLATE, modifyStringToComplyRegex(accountIdentifier));
        if (!isDataPresentPreAgg(dataSetName, CloudProvider.GCP.name())) {
          // Data not available at destination. Possibly an issue with CFs
          return ConnectorValidationResult.builder()
                  .errorSummary("Error with syncing data")
                  .status(ConnectivityStatus.FAILURE)
                  .build();
        }
      }
    } catch (Exception ex) {
      log.error("Unknown error occurred", ex);
      return ConnectorValidationResult.builder()
          .errorSummary("Unknown error occurred")
          .status(ConnectivityStatus.FAILURE)
          .build();
    }
    log.info("Validation successfull");
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  public String modifyStringToComplyRegex(String accountInfo) {
    return accountInfo.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  public  ConnectorValidationResult validateAccessToBillingReport(
      String projectId, String datasetId, String impersonatedServiceAccount) {
    boolean isTablePresent = false;
    ServiceAccountCredentials sourceCredentials = getGcpCredentials(GCP_CREDENTIALS_PATH);
    Credentials credentials = getGcpImpersonatedCredentials(sourceCredentials, impersonatedServiceAccount);
    BigQuery bigQuery;
    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(credentials);
    log.info(
        "projectId {}, datasetId {}, impersonatedServiceAccount {}", projectId, datasetId, impersonatedServiceAccount);
    if (projectId != null) {
      bigQueryOptionsBuilder.setProjectId(projectId);
    }
    bigQuery = bigQueryOptionsBuilder.build().getService();

    try {
      Dataset dataset = bigQuery.getDataset(datasetId);
      if (dataset == null) {
        log.error("Unable to find the dataset :" + datasetId);
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary("Unable to find the dataset " + datasetId + " in project " + projectId
                + ". Please check if dataset exists and service account " + impersonatedServiceAccount
                + " has required permissions")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      } else {
        log.info("dataset {} is present", datasetId);
        // Check for presence of table "gcp_billing_export_v1_*"
        Page<Table> tableList = dataset.list(BigQuery.TableListOption.pageSize(1000));
        for (Table table : tableList.getValues()) {
          if (table.getTableId().getTable().contains(GCP_BILLING_EXPORT_V_1)) {
            isTablePresent = true;
            log.info("table {} is present", table.getTableId().getTable());
          }
        }
        if (!isTablePresent) {
          return ConnectorValidationResult.builder()
              .status(ConnectivityStatus.PARTIAL)
              .errorSummary("Billing table " + GCP_BILLING_EXPORT_V_1 + "not yet present in"
                  + " the dataset " + datasetId + " in project " + projectId)
              .testedAt(Instant.now().toEpochMilli())
              .build();
        }
        return null;
      }
    } catch (BigQueryException be) {
      log.error("Unable to access BigQuery Dataset", be);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(
              ErrorDetail.builder().code(be.getCode()).reason(be.getMessage()).message(be.getMessage()).build()))
          .errorSummary("Unable to access the dataset " + datasetId + " in project " + projectId)
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
  }

  public  ServiceAccountCredentials getGcpCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    if (isEmpty(googleCredentialsPath)) {
      log.error("Missing environment variable for GCP credentials.");
    }
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials credentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      log.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      log.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }

  public  Credentials getGcpImpersonatedCredentials(
      ServiceAccountCredentials sourceCredentials, String impersonatedServiceAccount) {
    if (impersonatedServiceAccount == null) {
      return sourceCredentials;
    } else {
      return ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
    }
  }

  public  boolean isDataPresentPreAgg(String datasetId, String cloudProvider) {
    BigQuery bigquery = bigQueryService.get();
    String gcpProjectId = configuration.getGcpConfig().getGcpProjectId();
    String tablePrefix = gcpProjectId + "." + datasetId;
    String query = String.format(BQ_PRE_AGG_TABLE_DATACHECK_TEMPLATE, tablePrefix, cloudProvider);
    QueryJobConfiguration queryConfig =
            QueryJobConfiguration.newBuilder(query)
                    .addNamedParameter("run_date", QueryParameterValue.date(String.valueOf(java.time.LocalDate.now())))
                    .build();

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
