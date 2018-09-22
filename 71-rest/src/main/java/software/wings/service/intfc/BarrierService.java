package software.wings.service.intfc;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.BarrierInstance;
import software.wings.beans.OrchestrationWorkflow;

import java.util.List;
import javax.validation.Valid;

public interface BarrierService {
  String save(@Valid BarrierInstance barrier);
  BarrierInstance get(String barrierId);
  BarrierInstance update(String appId, String barrierId);

  String findByStep(String appId, String pipelineStateId, String workflowExecutionId, String identifier);

  @Value
  @Builder
  class OrchestrationWorkflowInfo {
    private String pipelineStateId;
    private String workflowId;
    private OrchestrationWorkflow orchestrationWorkflow;
  }

  List<BarrierInstance> obtainInstances(
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId);

  void updateAllActiveBarriers();
  void updateAllActiveBarriers(String appId);
}
