package software.wings.service.impl.yaml.sync;

import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncFailureAlertDetails {
  private String errorMessage;
  private ErrorCode errorCode;
  private String gitConnectorId;
  private String branchName;
}
