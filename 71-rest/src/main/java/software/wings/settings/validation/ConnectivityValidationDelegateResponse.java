package software.wings.settings.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

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