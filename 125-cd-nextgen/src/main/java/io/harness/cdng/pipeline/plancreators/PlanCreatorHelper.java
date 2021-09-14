/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.pipeline.plancreators;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.Nonnull;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class PlanCreatorHelper {
  @Inject private EngineExpressionService engineExpressionService;

  public boolean shouldNodeRun(@Nonnull RollbackNode rollbackNode, @Nonnull Ambiance ambiance) {
    if (rollbackNode.isShouldAlwaysRun()) {
      return true;
    }

    String value = engineExpressionService.renderExpression(
        ambiance, format("<+%s.status>", rollbackNode.getDependentNodeIdentifier()), true);
    return !EngineExpressionEvaluator.hasExpressions(value);
  }
}
