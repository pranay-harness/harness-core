package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.mongodb.DBObject;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

@FunctionalInterface
@OwnedBy(HarnessTeam.CE)
public interface ChangeStreamSubscriber {
  void onChange(ChangeStreamDocument<DBObject> changeStreamDocument);
}
