package io.harness.mongo;

import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "MorphiaMoveKeys")
@Entity(value = "morphiaMove", noClassnameStored = true)
public class MorphiaMove implements PersistentEntity {
  @Id private String target;
  private Set<String> sources;
}
