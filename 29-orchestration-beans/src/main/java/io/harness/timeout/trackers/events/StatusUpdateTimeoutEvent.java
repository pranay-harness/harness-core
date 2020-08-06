package io.harness.timeout.trackers.events;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.Status;
import io.harness.timeout.TimeoutEvent;
import lombok.AllArgsConstructor;
import lombok.Value;

@OwnedBy(CDC)
@Value
@AllArgsConstructor
public class StatusUpdateTimeoutEvent implements TimeoutEvent {
  public static final String EVENT_TYPE = "STATUS_UPDATE";

  Status status;

  @Override
  public String getType() {
    return EVENT_TYPE;
  }
}
