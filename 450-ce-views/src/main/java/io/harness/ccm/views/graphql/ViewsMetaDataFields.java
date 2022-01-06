/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
