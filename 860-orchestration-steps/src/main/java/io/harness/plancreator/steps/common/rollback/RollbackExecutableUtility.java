/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.steps.common.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.rollback.OnFailRollbackOutput;
import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class RollbackExecutableUtility {
  private final String ROLLBACK = "ROLLBACK";
  public void publishRollbackInfo(Ambiance ambiance, StepElementParameters stepParameters, Map<String, String> metadata,
      ExecutionSweepingOutputService executionSweepingOutputService) {
    OnFailRollbackParameters onFailRollbackParameters = stepParameters.getRollbackParameters();
    RollbackStrategy strategy;
    if (onFailRollbackParameters.getStrategy() == RollbackStrategy.UNKNOWN) {
      if (!metadata.containsKey(ROLLBACK)) {
        throw new InvalidRequestException(
            "Rollback strategy is not propagated to the step with parameters - " + stepParameters.toString());
      }
      strategy = RollbackStrategy.fromYamlName(metadata.get(ROLLBACK));
    } else {
      strategy = onFailRollbackParameters.getStrategy();
    }
    String nextNodeId = onFailRollbackParameters.getStrategyToUuid().get(strategy);
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(YAMLFieldNameConstants.USE_ROLLBACK_STRATEGY));
    if (!optionalSweepingOutput.isFound()) {
      executionSweepingOutputService.consume(ambiance, YAMLFieldNameConstants.USE_ROLLBACK_STRATEGY,
          OnFailRollbackOutput.builder().nextNodeId(nextNodeId).build(), YAMLFieldNameConstants.PIPELINE_GROUP);
    }
  }
}
