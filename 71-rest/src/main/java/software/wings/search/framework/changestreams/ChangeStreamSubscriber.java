package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import com.mongodb.client.model.changestream.ChangeStreamDocument;

@FunctionalInterface
public interface ChangeStreamSubscriber {
  void onChange(ChangeStreamDocument<DBObject> changeStreamDocument);
}
