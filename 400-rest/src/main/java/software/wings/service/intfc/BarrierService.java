/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.BarrierInstance;
import software.wings.beans.OrchestrationWorkflow;

import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
public interface BarrierService {
  String save(@Valid BarrierInstance barrier);
  BarrierInstance get(String barrierId);
  BarrierInstance update(String appId, String barrierId);
  BarrierInstance update(BarrierInstance barrierInstance);

  String findByStep(String appId, String pipelineStageId, int pipelineStageParallelIndex, String workflowExecutionId,
      String identifier);

  @Value
  @Builder
  class OrchestrationWorkflowInfo {
    private String pipelineStageId;
    private String workflowId;
    private boolean isLooped;
    private OrchestrationWorkflow orchestrationWorkflow;
  }

  List<BarrierInstance> obtainInstances(
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId, int parallelIndex);

  void updateAllActiveBarriers(String appId);
}
