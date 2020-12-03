package io.harness.delegate.task.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.expression.ExpressionEvaluator;

import com.amazonaws.regions.Regions;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AwsRegionCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    AwsRegionCapability awsRegionCapability = (AwsRegionCapability) delegateCapability;
    String region = awsRegionCapability.getRegion();
    boolean valid = region == null || checkIfSameRegion(region) || isLocalDev();
    return CapabilityResponse.builder().delegateCapability(awsRegionCapability).validated(valid).build();
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
