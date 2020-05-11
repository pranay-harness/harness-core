package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.BarrierInstance;
import software.wings.beans.OrchestrationWorkflow;

import java.util.List;
import javax.validation.Valid;

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
    private OrchestrationWorkflow orchestrationWorkflow;
  }

  List<BarrierInstance> obtainInstances(
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId, int parallelIndex);

  void updateAllActiveBarriers(String appId);
}
