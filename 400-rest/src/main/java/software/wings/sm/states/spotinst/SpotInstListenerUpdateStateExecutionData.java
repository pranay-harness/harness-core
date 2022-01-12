/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;

import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CDP)
public class SpotInstListenerUpdateStateExecutionData
    extends StateExecutionData implements DelegateTaskNotifyResponseData {
  private String appId;
  private String infraId;
  private String envId;
  private String serviceId;
  private String activityId;
  private boolean downsizeOldElastiGroup;
  private List<LoadBalancerDetailsForBGDeployment> lbDetails;
  private String prodTargetGroups;
  private String stageTargetGroups;
  private String commandName;
  private boolean isRollback;

  private String newElastiGroupId;
  private String newElastiGroupName;
  private String oldElastiGroupId;
  private String oldElastiGroupName;

  private SpotInstCommandRequest spotinstCommandRequest;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getInternalExecutionDetails();
  }

  private Map<String, ExecutionDataValue> getInternalExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    // putting activityId is very important, as without it UI wont make call to fetch commandLogs that are shown
    // in activity window
    putNotNull(executionDetails, "activityId",
        ExecutionDataValue.builder()
            .value(spotinstCommandRequest.getSpotInstTaskParameters().getActivityId())
            .displayName("Activity Id")
            .build());
    putNotNull(executionDetails, "prodTargetGroups",
        ExecutionDataValue.builder()
            .value(getActiveTargetGroups())
            .displayName("Active Target Groups (After Swap)")
            .build());
    putNotNull(executionDetails, "stageTargetGroups",
        ExecutionDataValue.builder()
            .value(getInactiveTargetGroups())
            .displayName("Stage Target Groups (After Swap)")
            .build());

    putNotNull(executionDetails, "downsizeOldElastiGroup",
        ExecutionDataValue.builder().value(downsizeOldElastiGroup).displayName("Downsize Old ElastiGroup").build());
    return executionDetails;
  }

  private String getActiveTargetGroups() {
    if (isEmpty(lbDetails)) {
      return StringUtils.EMPTY;
    }

    List<String> tgNames;
    if (isRollback) {
      // If rollback, we are restoring original prodTargetGroups.
      tgNames = lbDetails.stream()
                    .filter(Objects::nonNull)
                    .map(LoadBalancerDetailsForBGDeployment::getProdTargetGroupName)
                    .filter(Objects::nonNull)
                    .collect(toList());
    } else {
      tgNames = lbDetails.stream()
                    .filter(Objects::nonNull)
                    .map(LoadBalancerDetailsForBGDeployment::getStageListenerArn)
                    .filter(Objects::nonNull)
                    .collect(toList());
    }

    return returnTargetGroupDisplayString(tgNames);
  }

  private String getInactiveTargetGroups() {
    if (isEmpty(lbDetails)) {
      return StringUtils.EMPTY;
    }

    List<String> tgNames;
    if (isRollback) {
      // If rollback, we are restoring original prodTargetGroups.
      tgNames = lbDetails.stream()
                    .filter(Objects::nonNull)
                    .map(LoadBalancerDetailsForBGDeployment::getStageListenerArn)
                    .filter(Objects::nonNull)
                    .collect(toList());
    } else {
      tgNames = lbDetails.stream()
                    .filter(Objects::nonNull)
                    .map(LoadBalancerDetailsForBGDeployment::getProdTargetGroupName)
                    .filter(Objects::nonNull)
                    .collect(toList());
    }

    return returnTargetGroupDisplayString(tgNames);
  }

  private String returnTargetGroupDisplayString(List<String> tgNames) {
    if (isEmpty(tgNames)) {
      return StringUtils.EMPTY;
    }

    boolean isFirstElement = true;
    final StringBuilder stringBuilder = new StringBuilder(128);
    for (String name : tgNames) {
      if (isFirstElement) {
        stringBuilder.append(name);
        isFirstElement = false;
      } else {
        stringBuilder.append(" ,").append(name);
      }
    }

    return stringBuilder.toString();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    return getInternalExecutionDetails();
  }

  @Override
  public SpotinstDeployExecutionSummary getStepExecutionSummary() {
    return SpotinstDeployExecutionSummary.builder()
        .oldElastigroupId(oldElastiGroupId)
        .oldElastigroupName(oldElastiGroupName)
        .newElastigroupId(newElastiGroupId)
        .newElastigroupName(newElastiGroupName)
        .build();
  }
}
