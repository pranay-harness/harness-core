/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.graphql;

public enum ViewsMetaDataFields {
  TIME_GRANULARITY("time_granularity", "time_granularity"),
  INSTANCE_TYPE("instancetype", "instancetype"),
  START_TIME("startTime", "startTime"),
  START_TIME_MIN("startTime", "startTime_MIN"),
  START_TIME_MAX("startTime", "startTime_MAX"),
  COST("cost", "cost"),
  CLUSTER_COST("billingamount", "billingamount"),
  IDLE_COST("actualidlecost", "actualidlecost"),
  UNALLOCATED_COST("unallocatedcost", "unallocatedcost"),
  LABEL_KEY("labels.key", "labels_key"),
  LABEL_VALUE("labels.value", "labels_value"),
  LABEL_KEY_UN_NESTED("labelsUnnested.key", "labels_key"),
  LABEL_VALUE_UN_NESTED("labelsUnnested.value", "labels_value");

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
