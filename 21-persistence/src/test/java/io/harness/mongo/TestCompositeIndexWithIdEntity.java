package io.harness.mongo;

import io.harness.mongo.TestCompositeIndexWithIdEntity.TestCompositeIndexWithIdEntityKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
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
@FieldNameConstants(innerTypeName = "TestCompositeIndexWithIdEntityKeys")
@CdIndex(name = "index",
    fields = { @Field(TestCompositeIndexWithIdEntityKeys.uuid)
               , @Field(TestCompositeIndexWithIdEntityKeys.name) })
public class TestCompositeIndexWithIdEntity implements PersistentEntity, UuidAccess {
  @Id private String uuid;
  private String name;
}
