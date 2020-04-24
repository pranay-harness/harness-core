package software.wings.graphql.schema.mutation.execution.input;

import software.wings.graphql.schema.type.QLEnum;

public enum QLArtifactInputType implements QLEnum {
  ARTIFACT_ID,
  BUILD_NUMBER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}