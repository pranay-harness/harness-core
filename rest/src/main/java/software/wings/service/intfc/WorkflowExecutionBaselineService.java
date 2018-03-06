package software.wings.service.intfc;

import software.wings.beans.baseline.WorkflowExecutionBaseline;

import java.util.List;

/**
 * Created by rsingh on 2/16/18.
 */
public interface WorkflowExecutionBaselineService {
  void markBaseline(List<WorkflowExecutionBaseline> workflowExecutionBaselines, String executionId);

  String getBaselineExecutionId(String workflowId, String envId, String serviceId);
}
