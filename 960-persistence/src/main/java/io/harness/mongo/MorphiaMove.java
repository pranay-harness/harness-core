package io.harness.mongo;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@FieldNameConstants(innerTypeName = "MorphiaMoveKeys")
@Entity(value = "morphiaMove", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class MorphiaMove implements PersistentEntity {
  @Id private String target;
  private Set<String> sources;
}
