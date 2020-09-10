package software.wings.search.framework.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.mongodb.DBObject;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
@FunctionalInterface
public interface ChangeStreamSubscriber {
  void onChange(ChangeStreamDocument<DBObject> changeStreamDocument);
}
