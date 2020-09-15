package io.harness.delegate.task.gcp.taskHandlers;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import com.google.inject.Inject;

import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.gcp.client.GcpClient;
import io.harness.logging.CommandExecutionStatus;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@NoArgsConstructor
@Slf4j
public class GcpValidationTaskHandler implements TaskHandler {
  @Inject private GcpClient gcpClient;

  @Override
  public GcpResponse executeRequest(GcpRequest gcpRequest) {
    try {
      gcpClient.validateDefaultCredentials();
      return GcpValidationTaskResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build();
    } catch (Exception ex) {
      logger.error("Failed while validating default credentials for GCP", ex);
      return GcpValidationTaskResponse.builder()
          .executionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage() + "." + getRootCauseMessage(ex))
          .build();
    }
  }

  private String getRootCauseMessage(Throwable t) {
    return Optional.ofNullable(getRootCause(t)).map(Throwable::getMessage).orElse(StringUtils.EMPTY);
  }
}
