package io.harness.mongo;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;
import io.harness.queue.Queuable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageLogContext extends AutoLogContext {
  public static final String MESSAGE_CLASS = "messageClass";
  public static final String MESSAGE_ID = "messageId";
  public static final String MESSAGE_TOPIC = "messageTopic";

  public MessageLogContext(Queuable message, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put(MESSAGE_CLASS, message.getClass().getName())
              .putIfNotNull(MESSAGE_ID, message.getId())
              .putIfNotNull(MESSAGE_TOPIC, message.getTopic())
              .build(),
        behavior);
  }
}
