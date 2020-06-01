package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class StatusNotifyResponseData implements ResponseData {
  Status status;
}
