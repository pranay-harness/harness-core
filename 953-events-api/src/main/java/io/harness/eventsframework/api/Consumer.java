package io.harness.eventsframework.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

import java.time.Duration;
import java.util.List;

@OwnedBy(PL)
public interface Consumer {
  List<Message> read(Duration maxWaitTime);
  void acknowledge(String messageId);
  void shutdown();
}
