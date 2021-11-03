package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.manager.CENextGenResourceClient;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.ErrorDetail;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@OwnedBy(CE)
public class CEGcpConnectorValidator extends AbstractConnectorValidator {
  @Autowired
  private CENextGenResourceClient ceNextGenResourceClient;
  public static final String GCP_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  public static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return (ConnectorValidationResult) ceNextGenResourceClient.testConnection(accountIdentifier, connectorResponseDTO);
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }
}
