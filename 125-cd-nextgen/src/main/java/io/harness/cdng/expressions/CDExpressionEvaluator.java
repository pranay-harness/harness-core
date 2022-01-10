/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.expressions;

import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import java.util.Set;

public class CDExpressionEvaluator extends AmbianceExpressionEvaluator {
  public CDExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }

  @Override
  protected void initialize() {
    super.initialize();
    //    addToContext("account", new AccountFunctor(accountService, ambiance));
    // TODO(archit): Add new AccountService when done for NG
    addStaticAlias("artifact", "service.artifacts.primary.output");
    addStaticAlias("serviceVariables", "service.variables.output");
    addStaticAlias("env", "infrastructure.environment");
    addGroupAlias(YAMLFieldNameConstants.STAGE, StepOutcomeGroup.STAGE.name());
    addGroupAlias(YAMLFieldNameConstants.STEP, StepOutcomeGroup.STEP.name());
  }
}
