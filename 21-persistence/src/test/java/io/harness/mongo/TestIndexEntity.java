package io.harness.mongo;

import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.SparseIndex;
import io.harness.mongo.index.TtlIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestEntityKeys")
@Index(name = "index",
    fields = { @Field(TestIndexEntity.TestEntityKeys.name)
               , @Field(TestIndexEntity.TestEntityKeys.test) })
@SparseIndex(name = "sparse_index",
    fields = { @Field(TestIndexEntity.TestEntityKeys.name)
               , @Field(TestIndexEntity.TestEntityKeys.sparseTest) })
public class TestIndexEntity implements PersistentEntity, UuidAccess {
  @Id private String uuid;
  private String name;
  @Indexed private String test;
  @Indexed(sparse = true) private String sparseTest;
  @TtlIndex(11) private String ttlTest;
}