package software.wings.service.impl.aws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AwsResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
  private String errorMessage;
}