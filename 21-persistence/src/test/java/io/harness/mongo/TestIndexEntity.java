package io.harness.mongo;

import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdSparseIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdSparseIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

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
@CdIndex(name = "index",
    fields = { @Field(TestIndexEntity.TestEntityKeys.name)
               , @Field(TestIndexEntity.TestEntityKeys.test) })
@CdSparseIndex(name = "sparse_index",
    fields = { @Field(TestIndexEntity.TestEntityKeys.name)
               , @Field(TestIndexEntity.TestEntityKeys.sparseTest) })
@Entity(value = "!!!testIndexes", noClassnameStored = true)
public class TestIndexEntity implements PersistentEntity, UuidAccess {
  @Id private String uuid;
  private String name;
  @FdIndex private String test;
  @FdUniqueIndex private String uniqueTest;
  @FdSparseIndex private String sparseTest;
  @FdTtlIndex(11) private String ttlTest;
}
