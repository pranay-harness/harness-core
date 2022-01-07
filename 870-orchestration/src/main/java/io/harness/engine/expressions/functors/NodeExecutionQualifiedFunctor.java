/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.expression.LateBindingValue;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class NodeExecutionQualifiedFunctor implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  PmsOutcomeService pmsOutcomeService;
  PmsSweepingOutputService pmsSweepingOutputService;
  Ambiance ambiance;
  Set<NodeExecutionEntityType> entityTypes;

  @Override
  public Object bind() {
    return NodeExecutionValue.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .pmsOutcomeService(pmsOutcomeService)
        .pmsSweepingOutputService(pmsSweepingOutputService)
        .ambiance(ambiance)
        .startNodeExecution(null)
        .entityTypes(entityTypes)
        .build()
        .bind();
  }
}
