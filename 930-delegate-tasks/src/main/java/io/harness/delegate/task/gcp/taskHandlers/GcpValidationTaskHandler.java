package io.harness.delegate.task.gcp.taskHandlers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.gcp.client.GcpClient;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Slf4j
public class GcpValidationTaskHandler implements TaskHandler {
  @Inject private GcpClient gcpClient;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private NGErrorHelper ngErrorHelper;

  @Override
  public GcpResponse executeRequest(GcpRequest gcpRequest) {
    try {
      if (isNotBlank(gcpRequest.getDelegateSelector())) {
        gcpClient.validateDefaultCredentials();
        ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                                  .status(ConnectivityStatus.SUCCESS)
                                                                  .testedAt(System.currentTimeMillis())
                                                                  .build();
        return GcpValidationTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
      } else {
        return validateGcpServiceAccountKeyCredential(gcpRequest);
      }
    } catch (Exception ex) {
      log.error("Failed while validating credentials for GCP", ex);
      return getFailedGcpResponse(ex);
    }
  }

  private GcpResponse validateGcpServiceAccountKeyCredential(GcpRequest gcpRequest) {
    GcpManualDetailsDTO gcpManualDetailsDTO = gcpRequest.getGcpManualDetailsDTO();
    if (gcpManualDetailsDTO == null || gcpManualDetailsDTO.getSecretKeyRef() == null) {
      throw new InvalidRequestException("Authentication details not found");
    }
    secretDecryptionService.decrypt(gcpManualDetailsDTO, gcpRequest.getEncryptionDetails());
    gcpClient.getGkeContainerService(gcpManualDetailsDTO.getSecretKeyRef().getDecryptedValue());
    ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                              .status(ConnectivityStatus.SUCCESS)
                                                              .testedAt(System.currentTimeMillis())
                                                              .build();
    return GcpValidationTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }

  private GcpValidationTaskResponse getFailedGcpResponse(Exception ex) {
    ConnectorValidationResult connectorValidationResult =
        ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .testedAt(System.currentTimeMillis())
            .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(ex.getMessage())))
            .errorSummary(ngErrorHelper.getErrorSummary(ex.getMessage()))
            .build();
    return GcpValidationTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }

  private String getRootCauseMessage(Throwable t) {
    return Optional.ofNullable(getRootCause(t)).map(Throwable::getMessage).orElse(StringUtils.EMPTY);
  }
}
