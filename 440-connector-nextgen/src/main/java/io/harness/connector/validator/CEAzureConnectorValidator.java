package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.connector.ConnectivityStatus.FAILURE;
import static io.harness.errorhandling.NGErrorHelper.DEFAULT_ERROR_SUMMARY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.manager.CENextGenResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.utils.RestCallToNGManagerClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class CEAzureConnectorValidator extends AbstractConnectorValidator {
  @Inject CENextGenResourceClient ceNextGenResourceClient;
  @Inject NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    log.info("Calling ce-nextgen for connector {}", connectorResponseDTO);
    try {
      return RestCallToNGManagerClientUtils.execute(
          ceNextGenResourceClient.testConnection(accountIdentifier, connectorResponseDTO));
    } catch (InvalidRequestException | UnexpectedException ex) {
      log.info("Error could be in CENG microservice or in connecting to it");
      List<ErrorDetail> errorDetails = Collections.singletonList(ngErrorHelper.getGenericErrorDetail());
      return ConnectorValidationResult.builder()
          .errors(errorDetails)
          .errorSummary(DEFAULT_ERROR_SUMMARY)
          .testedAt(System.currentTimeMillis())
          .status(FAILURE)
          .build();
    }
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
