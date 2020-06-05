package io.harness.cdng.artifact.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DockerArtifactKeys")
@EqualsAndHashCode(callSuper = true)
public class DockerArtifact extends Artifact {
  /** Docker hub registry connector uuid. */
  @NotEmpty private String dockerHubConnectorId;

  /** Images in repos need to be referenced via a path */
  @NotEmpty private String imagePath;

  /** Tag refers to exact tag number */
  @NotEmpty private String tag;
}
