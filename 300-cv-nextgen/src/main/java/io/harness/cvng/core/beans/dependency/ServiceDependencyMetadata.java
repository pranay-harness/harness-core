package io.harness.cvng.core.beans.dependency;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "ServiceDependencyMetadataKeys")
@NoArgsConstructor
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class ServiceDependencyMetadata {
  private DependencyMetadataType type;

  public abstract Set<ChangeSourceType> getSupportedChangeSourceTypes();

  enum DependencyMetadataType { KUBERNETES; }
}
