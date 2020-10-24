package io.harness.ccm.views.graphql;

public enum ViewsMetaDataFields {
  TIME_GRANULARITY("time_granularity", "time_granularity"),
  INSTANCE_TYPE("instancetype", "instancetype"),
  START_TIME("startTime", "startTime"),
  START_TIME_MIN("startTime", "startTime_MIN"),
  START_TIME_MAX("startTime", "startTime_MAX"),
  COST("cost", "cost"),
  LABEL_KEY("labels.key", "labels_key"),
  LABEL_VALUE("labels.value", "labels_value");

  private String fieldName;
  private String alias;

  ViewsMetaDataFields(String fieldName, String alias) {
    this.fieldName = fieldName;
    this.alias = alias;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getAlias() {
    return alias;
  }
}
