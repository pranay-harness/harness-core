package software.wings.search.framework.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.mongodb.DBObject;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

@OwnedBy(PL)
@FunctionalInterface
public interface ChangeStreamSubscriber {
  void onChange(ChangeStreamDocument<DBObject> changeStreamDocument);
}
