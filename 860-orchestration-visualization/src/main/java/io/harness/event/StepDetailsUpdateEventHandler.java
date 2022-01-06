/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class StepDetailsUpdateEventHandler {
  @Inject PmsGraphStepDetailsService pmsGraphStepDetailsService;

  public OrchestrationGraph handleEvent(
      String planExecutionId, String nodeExecutionId, OrchestrationGraph orchestrationGraph) {
    try {
      orchestrationGraph.getAdjacencyList()
          .getGraphVertexMap()
          .get(nodeExecutionId)
          .setStepDetails(pmsGraphStepDetailsService.getStepDetails(planExecutionId, nodeExecutionId));
      return orchestrationGraph;
    } catch (Exception e) {
      log.error("Graph update for Step Details update event failed for node [{}]", nodeExecutionId, e);
      throw e;
    }
  }
}
