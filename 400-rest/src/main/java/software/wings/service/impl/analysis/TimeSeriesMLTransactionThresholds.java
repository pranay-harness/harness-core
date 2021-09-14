/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.metrics.TimeSeriesCustomThresholdType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "TimeSeriesMLTransactionThresholdKeys")
@Entity(value = "timeseriesTransactionThresholds", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesMLTransactionThresholds extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("timeseriesThresholdsQueryIndex")
                 .field("appId")
                 .field(TimeSeriesMLTransactionThresholdKeys.serviceId)
                 .field(TimeSeriesMLTransactionThresholdKeys.stateType)
                 .field(TimeSeriesMLTransactionThresholdKeys.groupName)
                 .field(TimeSeriesMLTransactionThresholdKeys.transactionName)
                 .field(TimeSeriesMLTransactionThresholdKeys.metricName)
                 .field(TimeSeriesMLTransactionThresholdKeys.cvConfigId)
                 .field(TimeSeriesMLTransactionThresholdKeys.thresholdType)
                 .build())
        .build();
  }
  @NotEmpty private String serviceId;

  @NotEmpty private String workflowId;

  @NotEmpty private StateType stateType;

  @NotEmpty private String groupName;

  @NotEmpty private String transactionName;

  @NotEmpty private String metricName;

  @NotEmpty private String cvConfigId;

  @FdIndex private String accountId;

  TimeSeriesMetricDefinition thresholds;

  TimeSeriesCustomThresholdType thresholdType = TimeSeriesCustomThresholdType.ACCEPTABLE;

  private String customThresholdRefId;

  private int version;

  public TimeSeriesMLTransactionThresholds cloneWithoutCustomThresholds() {
    return TimeSeriesMLTransactionThresholds.builder()
        .serviceId(serviceId)
        .workflowId(workflowId)
        .stateType(stateType)
        .groupName(groupName)
        .transactionName(transactionName)
        .metricName(metricName)
        .cvConfigId(cvConfigId)
        .accountId(accountId)
        .thresholds(TimeSeriesMetricDefinition.builder()
                        .metricName(thresholds.getMetricName())
                        .metricType(thresholds.getMetricType())
                        .tags(thresholds.getTags() != null ? Sets.newHashSet(thresholds.getTags()) : null)
                        .categorizedThresholds(thresholds.getCategorizedThresholds() != null
                                ? new HashMap<>(thresholds.getCategorizedThresholds())
                                : null)
                        .customThresholds(new ArrayList<>())
                        .build())
        .build();
  }
}
