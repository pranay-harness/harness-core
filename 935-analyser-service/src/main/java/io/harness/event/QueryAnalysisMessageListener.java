package io.harness.event;

import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;

public class QueryAnalysisMessageListener implements MessageListener {
  // Todo: Once finalized on the object, add handling from HMongoTemplate here.
  @Override
  public boolean handleMessage(Message message) {
    return true;
  }
}
