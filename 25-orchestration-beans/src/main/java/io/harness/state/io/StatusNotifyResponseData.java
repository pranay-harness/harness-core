package io.harness.state.io;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class StatusNotifyResponseData implements ResponseData {
  NodeExecutionStatus status;
}
