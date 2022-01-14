package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLEnvironmentType implements QLEnum {
  PROD,
  NON_PROD;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
