/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mongo.index;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
public class CompoundTextMongoIndex implements MongoIndex {
  String name;
  boolean sparse;
  @Singular private List<String> fields;
  @Singular private List<String> textFields;
  @Singular private List<String> rangeFields;

  @Override
  public IndexCreatorBuilder createBuilder(String id) {
    checks(log);
    BasicDBObject keys = buildBasicDBObject(id);
    BasicDBObject options = buildBasicDBObject();
    if (isNotEmpty(getTextFields())) {
      for (String field : getTextFields()) {
        if (field.equals(id)) {
          throw new IndexManagerInspectException("There is no point of having collection key in a composite index."
              + "\nIf in the query there is a unique value it will always fetch exactly one item");
        }
        keys.append(field, IndexType.TEXT.toIndexValue());
      }
    }
    if (isNotEmpty(getRangeFields())) {
      for (String field : getRangeFields()) {
        if (field.equals(id)) {
          throw new IndexManagerInspectException("There is no point of having collection key in a composite index."
              + "\nIf in the query there is a unique value it will always fetch exactly one item");
        }
        keys.append(field, IndexType.ASC.toIndexValue());
      }
    }
    return IndexCreator.builder().keys(keys).options(options);
  }

  @Override
  public boolean isUnique() {
    return false;
  }
}
