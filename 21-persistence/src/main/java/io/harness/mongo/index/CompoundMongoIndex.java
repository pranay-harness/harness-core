package io.harness.mongo.index;

import com.mongodb.BasicDBObject;
import io.harness.mongo.IndexCreator;
import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import io.harness.mongo.IndexManagerInspectException;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Builder
@Slf4j
public class CompoundMongoIndex implements MongoIndex {
  private String name;
  @Singular private List<String> fields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    BasicDBObject keys = new BasicDBObject();

    if (fields.size() == 1 && !fields.get(0).contains(".")) {
      logger.error("Composite index with only one field {}", fields.get(0));
    }

    for (String field : fields) {
      if (field.equals(id)) {
        throw new IndexManagerInspectException("There is no point of having collection key in a composite index."
            + "\nIf in the query there is a unique value it will always fetch exactly one item");
      }
      keys.append(field, IndexType.ASC.toIndexValue());
    }

    BasicDBObject options = new BasicDBObject();
    options.put(NAME, name);
    options.put(BACKGROUND, Boolean.TRUE);
    return IndexCreator.builder().keys(keys).options(options);
  }
}
