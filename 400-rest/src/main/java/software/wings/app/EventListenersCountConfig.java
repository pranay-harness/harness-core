package software.wings.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class EventListenersCountConfig {
  int deploymentEventListenerCount;
  int instanceEventListenerCount;
  int deploymentTimeSeriesEventListenerCount;
  int executionEventListenerCount;
  int generalNotifyEventListenerCount;
  int orchestrationNotifyEventListenerCount;
}
