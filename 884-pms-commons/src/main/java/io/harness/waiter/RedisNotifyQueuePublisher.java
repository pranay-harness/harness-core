package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.waiter.notify.NotifyEventProto;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class RedisNotifyQueuePublisher implements NotifyQueuePublisher {
  private Producer producer;

  public RedisNotifyQueuePublisher(Producer producer) {
    this.producer = producer;
  }

  @Override
  public void send(NotifyEvent payload) {
    producer.send(Message.newBuilder().setData(toProto(payload).toByteString()).build());
  }

  public NotifyEventProto toProto(NotifyEvent event) {
    return NotifyEventProto.newBuilder().setWaitInstanceId(event.getWaitInstanceId()).build();
  }
}
