package io.harness.mongo.index;

import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import io.harness.mongo.IndexManagerInspectException;

import com.mongodb.BasicDBObject;
import java.util.List;
import org.slf4j.Logger;

public interface MongoIndex {
  String NAME = "name";
  String UNIQUE = "unique";
  String BACKGROUND = "background";

  IndexCreatorBuilder createBuilder(String id);
  String getName();
  boolean isUnique();
  List<String> getFields();

  default void checks(Logger log) {
    if (getFields().size() == 1 && !getFields().get(0).contains(".")) {
      log.error("Composite index with only one field {}", getFields().get(0));
    }

    if (isUnique() && !getName().startsWith("unique")) {
      log.error("Index {} is unique indexes and its name is not prefixed with unique", getName());
    }
  }

  default BasicDBObject buildBasicDBObject(String id) {
    BasicDBObject keys = new BasicDBObject();

    for (String field : getFields()) {
      if (field.equals(id)) {
        throw new IndexManagerInspectException("There is no point of having collection key in a composite index."
            + "\nIf in the query there is a unique value it will always fetch exactly one item");
      }
      keys.append(field, IndexType.ASC.toIndexValue());
    }
    return keys;
  }

  default BasicDBObject buildBasicDBObject() {
    BasicDBObject options = new BasicDBObject();
    options.put(NAME, getName());
    if (isUnique()) {
      options.put(UNIQUE, Boolean.TRUE);
    } else {
      options.put(BACKGROUND, Boolean.TRUE);
    }
    return options;
  }
}
