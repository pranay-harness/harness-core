package software.wings.api.cloudformation;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StateExecutionData;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ServiceNowExecutionData extends StateExecutionData implements ResponseData {
  private ExecutionStatus executionStatus;
  private String issueUrl;
  private String message;
}
