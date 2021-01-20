package io.harness.delegate.beans;

import io.harness.EntityType;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

/**
 * IMPORTANT: This class is a contract between backend and NG UI. If any field from this class is removed or added it
 * will affect json schema expected by NG UI. In case there are any field changes, maven compile goal must be executed
 * in order to generate new schemas. Generated schemas should be committed as part of the PR.
 */
@Data
@Builder
@AllArgsConstructor
@JsonTypeName("DELEGATE")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("delegate")
@YamlSchemaRoot(EntityType.DELEGATES)
public class DelegateSetupDetails {
  private String sessionIdentifier;
  @NotNull private String name;
  private String description;
  @NotNull private DelegateSize size;
  @NotNull private String delegateConfigurationId;
}
