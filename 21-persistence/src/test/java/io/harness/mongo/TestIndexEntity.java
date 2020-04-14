package io.harness.mongo;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestEntityKeys")
@Indexes({
  @Index(options = @IndexOptions(name = "index"),
      fields = { @Field(TestIndexEntity.TestEntityKeys.uuid)
                 , @Field(TestIndexEntity.TestEntityKeys.test) })
  ,
      @Index(options = @IndexOptions(name = "sparse_index", sparse = true), fields = {
        @Field(TestIndexEntity.TestEntityKeys.uuid), @Field(TestIndexEntity.TestEntityKeys.sparseTest)
      })
})
class TestIndexEntity implements PersistentEntity, UuidAccess {
  @Id private String uuid;
  @Indexed private String test;
  @Indexed(options = @IndexOptions(sparse = true)) private String sparseTest;
}
