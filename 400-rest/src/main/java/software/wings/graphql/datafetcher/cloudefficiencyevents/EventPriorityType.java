package software.wings.graphql.datafetcher.cloudefficiencyevents;

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
