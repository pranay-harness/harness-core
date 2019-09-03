package software.wings.sm;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ElementNotifyResponseData implements ExecutionStatusResponseData {
  private ExecutionStatus executionStatus;
  private List<ContextElement> contextElements;
}
