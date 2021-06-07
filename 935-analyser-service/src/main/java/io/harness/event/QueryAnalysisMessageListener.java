package io.harness.event;

import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryAnalysisMessageListener implements MessageListener {
  // Todo: Once finalized on the object, add handling from HMongoTemplate here.
  @Override
  public boolean handleMessage(Message message) {
    log.debug("Message data : {}", message.getMessage().getData().toStringUtf8());
    return true;
  }
}
