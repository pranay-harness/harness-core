package software.wings.service.intfc;

import software.wings.beans.ExecutionArgs;
import software.wings.beans.PipelineExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 10/26/16.
 */
public interface PipelineService {
  /**
   * List pipeline executions page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest);

  /**
   * Execute workflow execution.
   *
   * @param appId      the app id
   * @param pipelineId the pipeline id
   * @param executionArgs
   * @return the workflow execution
   */
  WorkflowExecution execute(String appId, String pipelineId, ExecutionArgs executionArgs);

  /**
   * Refresh pipeline execution.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   */
  void refreshPipelineExecution(String appId, String workflowExecutionId);

  /**
   * Refresh pipeline execution.
   *
   * @param appId               the app id
   * @param workflowExecutionId the workflow execution id
   */
  void refreshPipelineExecutionAsync(String appId, String workflowExecutionId);
}
