package software.wings.settings.validation;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityValidationDelegateResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private boolean valid;
  private String errorMessage;
  private ExecutionStatus executionStatus;
}