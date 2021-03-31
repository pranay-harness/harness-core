package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public enum EventPriorityType {
  notable("NOTABLE"),
  normal("NORMAL");

  private String fieldName;

  EventPriorityType(String fieldName) {
    this.fieldName = fieldName;
  }
  public String getFieldName() {
    return fieldName;
  }
}
