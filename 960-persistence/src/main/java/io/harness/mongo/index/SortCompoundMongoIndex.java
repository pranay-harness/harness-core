package io.harness.mongo.index;

import static org.atmosphere.annotation.AnnotationUtil.logger;

import io.harness.mongo.IndexCreator;
import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import io.harness.mongo.IndexManagerInspectException;

import com.mongodb.BasicDBObject;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
public class SortCompoundMongoIndex implements MongoIndex {
  private String name;
  private boolean unique;
  @Singular private List<String> fields;
  @Singular private List<String> sortFields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    checks(logger);

    BasicDBObject keys = buildBasicDBObject(id);
    for (String field : getSortFields()) {
      if (field.equals(id)) {
        throw new IndexManagerInspectException("There is no point of having collection key in a composite index."
            + "\nIf in the query there is a unique value it will always fetch exactly one item");
      }
      if (field.charAt(0) != '-') {
        keys.append(field, IndexType.ASC.toIndexValue());
      } else {
        keys.append(field.substring(1), IndexType.DESC.toIndexValue());
      }
    }

    BasicDBObject options = buildBasicDBObject();
    return IndexCreator.builder().keys(keys).options(options);
  }

  public static class SortCompoundMongoIndexBuilder {
    public SortCompoundMongoIndexBuilder ascSortField(String sortField) {
      return sortField(sortField);
    }

    public SortCompoundMongoIndexBuilder descSortField(String sortField) {
      return sortField("-" + sortField);
    }
  }
}
