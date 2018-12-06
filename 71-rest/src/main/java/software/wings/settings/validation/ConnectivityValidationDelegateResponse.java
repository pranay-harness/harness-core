package software.wings.settings.validation;

import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConnectivityValidationDelegateResponse extends DelegateTaskNotifyResponseData {
  private boolean valid;
  private String errorMessage;
  private ExecutionStatus executionStatus;
}