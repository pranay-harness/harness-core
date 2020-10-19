package io.harness.mongo.index;

import static org.atmosphere.annotation.AnnotationUtil.logger;

import com.mongodb.BasicDBObject;
import io.harness.mongo.IndexCreator;
import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.List;

@Value
@Builder
@Slf4j
public class CompoundMongoIndex implements MongoIndex {
  private String name;
  private boolean unique;
  @Singular private List<String> fields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    checks(logger);

    BasicDBObject keys = buildBasicDBObject(id);
    BasicDBObject options = buildBasicDBObject();
    return IndexCreator.builder().keys(keys).options(options);
  }
}
