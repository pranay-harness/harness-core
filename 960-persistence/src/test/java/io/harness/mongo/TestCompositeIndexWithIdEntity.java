package io.harness.mongo;

import io.harness.mongo.index.CompoundMongoIndex;
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
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TestCompositeIndexWithIdEntityKeys")
public class TestCompositeIndexWithIdEntity implements PersistentEntity, UuidAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("index")
                 .field(TestCompositeIndexWithIdEntityKeys.uuid)
                 .field(TestCompositeIndexWithIdEntityKeys.name)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String name;
}
