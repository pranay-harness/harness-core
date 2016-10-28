package software.wings.service.intfc;

import software.wings.beans.PipelineExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.sm.ExecutionStatus;

/**
 * Created by anubhaw on 10/26/16.
 */
public interface PipelineService {
  PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest, String appId);

  void updatePipelineExecutionData(String appId, String workflowExecutionId, ExecutionStatus status);
}
