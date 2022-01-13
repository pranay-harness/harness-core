/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Set;

@OwnedBy(CDC)
public class CDExpressionEvaluatorProvider implements ExpressionEvaluatorProvider {
  @Override
  public EngineExpressionEvaluator get(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    return new CDExpressionEvaluator(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }
}
