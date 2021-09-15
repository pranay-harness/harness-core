package io.harness.cvng.core.beans.dependency;

import static io.harness.cvng.core.beans.dependency.ServiceDependencyMetadata.DependencyMetadataType.KUBERNETES;

import io.harness.cvng.beans.change.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@JsonTypeName("KUBERNETES")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "KubernetesDependencyMetadataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KubernetesDependencyMetadata extends ServiceDependencyMetadata {
  String namespace;
  String workload;

  @Override
  public DependencyMetadataType getType() {
    return KUBERNETES;
  }

  @Override
  public Set<ChangeSourceType> getSupportedChangeSourceTypes() {
    return Sets.newHashSet(ChangeSourceType.KUBERNETES);
  }
}
