package io.harness.persistence.converters;

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
@FieldNameConstants(innerTypeName = "ObjectArrayTestEntityKeys")
public class ObjectArrayTestEntity implements PersistentEntity, UuidAccess {
  @Id String uuid;
  Object[] array;
}
