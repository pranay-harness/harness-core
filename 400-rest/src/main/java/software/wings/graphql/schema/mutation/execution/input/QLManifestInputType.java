package software.wings.graphql.schema.mutation.execution.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(HarnessTeam.CDC)
public enum QLManifestInputType implements QLEnum {
  HELM_CHART_ID,
  VERSION_NUMBER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
