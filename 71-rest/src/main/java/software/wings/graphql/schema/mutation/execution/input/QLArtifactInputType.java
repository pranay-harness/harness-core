package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CDC)
public enum QLArtifactInputType implements QLEnum {
  ARTIFACT_ID,
  BUILD_NUMBER,
  PARAMETERIZED_ARTIFACT_SOURCE;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
