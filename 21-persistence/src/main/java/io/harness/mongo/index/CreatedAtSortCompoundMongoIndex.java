package io.harness.mongo.index;

import static org.atmosphere.annotation.AnnotationUtil.logger;

import com.mongodb.BasicDBObject;
import io.harness.mongo.IndexCreator;
import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import io.harness.persistence.CreatedAtAware;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Value
@Builder
@Slf4j
public class CreatedAtSortCompoundMongoIndex implements MongoIndex {
  private String name;
  private boolean unique;
  @Singular private List<String> fields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    checks(logger);

    BasicDBObject keys = buildBasicDBObject(id);
    keys.put(CreatedAtAware.CREATED_AT_KEY, IndexType.DESC.toIndexValue());

    BasicDBObject options = buildBasicDBObject();
    return IndexCreator.builder().keys(keys).options(options);
  }
}
