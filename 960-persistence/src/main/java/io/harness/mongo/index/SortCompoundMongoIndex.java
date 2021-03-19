package io.harness.mongo.index;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.atmosphere.annotation.AnnotationUtil.logger;

import io.harness.mongo.IndexCreator;
import io.harness.mongo.IndexCreator.IndexCreatorBuilder;
import io.harness.mongo.IndexManagerInspectException;
import io.harness.serializer.JsonUtils;

import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import java.util.List;
import java.util.Objects;
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
  Collation collation;
  @Singular private List<String> fields;
  @Singular private List<String> sortFields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    Preconditions.checkState(isNotEmpty(name), name);
    Preconditions.checkState(!sortFields.isEmpty(), name);
    Preconditions.checkState(!fields.isEmpty(), name);

    checks(logger);

    BasicDBObject keys = buildBasicDBObject(id);
    BasicDBObject options = buildBasicDBObject();
    if (Objects.nonNull(collation)) {
      io.harness.mongo.Collation collation1 = io.harness.mongo.Collation.builder()
                                                  .locale(this.getCollation().getLocale().getCode())
                                                  .strength(this.getCollation().getStrength().getCode())
                                                  .build();
      BasicDBObject basicDBObject = BasicDBObject.parse(JsonUtils.asJson(collation1));
      options.put("collation", basicDBObject);
    }

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
