package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
