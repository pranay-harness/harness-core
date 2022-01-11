/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import com.amazonaws.regions.Regions;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsRegionCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    AwsRegionCapability awsRegionCapability = (AwsRegionCapability) delegateCapability;
    String region = awsRegionCapability.getRegion();
    boolean valid = region == null || checkIfSameRegion(region) || isLocalDev();
    return CapabilityResponse.builder().delegateCapability(awsRegionCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.AWS_REGION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    String region = parameters.getAwsRegionParameters().getRegion();
    boolean valid = region == null || checkIfSameRegion(region) || isLocalDev();
    return builder.permissionResult(valid ? PermissionResult.ALLOWED : PermissionResult.DENIED).build();
  }

  private boolean checkIfSameRegion(String region) {
    com.amazonaws.regions.Region regionForContainer = Regions.getCurrentRegion();
    if (regionForContainer != null) {
      return regionForContainer.getName().equals(region);
    }
    // When delegate is running as fargate task, rely on ENV variable: AWS_REGION
    String currentRegion = System.getenv("AWS_REGION");
    log.info("[Delegate Capability] ECS Current Region Value from ENV var {AWS_REGION}: " + currentRegion);
    if (isNotBlank(currentRegion)) {
      return currentRegion.equals(region);
    }

    log.info("[Delegate Capability] Failed in ECS validation, failed to fetch current region");
    return false;
  }

  private static boolean isLocalDev() {
    return !new File("start.sh").exists();
  }
}
