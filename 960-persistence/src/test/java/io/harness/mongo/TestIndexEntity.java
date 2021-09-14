/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdSparseIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestEntityKeys")
@Entity(value = "!!!testIndexes", noClassnameStored = true)
public class TestIndexEntity implements PersistentEntity, UuidAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder().name("index").field(TestEntityKeys.name).field(TestEntityKeys.test).build())
        .add(CompoundMongoIndex.builder()
                 .name("sparse_index")
                 .sparse(true)
                 .field(TestEntityKeys.name)
                 .field(TestEntityKeys.sparseTest)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String name;
  @FdIndex private String test;
  @FdUniqueIndex private String uniqueTest;
  @FdSparseIndex private String sparseTest;
  @FdTtlIndex(11) private String ttlTest;
}
