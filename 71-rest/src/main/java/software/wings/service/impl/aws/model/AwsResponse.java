package software.wings.service.impl.aws.model;

import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.ExecutionStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AwsResponse extends DelegateTaskNotifyResponseData {
  private ExecutionStatus executionStatus;
  private String errorMessage;
}