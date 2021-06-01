package io.harness.aggregator.consumers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.debezium.engine.ChangeEvent;

@OwnedBy(HarnessTeam.PL)
public interface ChangeEventFailureHandler {
  void handle(ChangeEvent<String, String> changeEvent, Exception exception);
}
