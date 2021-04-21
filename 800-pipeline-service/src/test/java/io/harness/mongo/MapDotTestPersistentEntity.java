package io.harness.mongo;

import io.harness.persistence.PersistentEntity;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("mapDotTestPersistentEntities")
@TypeAlias("mapDotTestPersistentEntities")
public class MapDotTestPersistentEntity implements PersistentEntity {
  @Id String id;
  @Singular Map<String, Object> keyValPairs;
}
