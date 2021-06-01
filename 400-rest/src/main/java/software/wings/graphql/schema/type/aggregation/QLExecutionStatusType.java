package software.wings.graphql.schema.type.aggregation;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CV)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLExecutionStatusType implements QLEnum {
  ABORTED,
  ERROR,
  FAILED,
  RUNNING,
  SUCCESS,
  SKIPPED,
  EXPIRED;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
