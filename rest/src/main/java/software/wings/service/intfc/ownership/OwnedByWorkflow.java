package software.wings.service.intfc.ownership;

public interface OwnedByWorkflow {
  /**
   * Prune if belongs to workflow.
   *
   * @param appId the app id
   * @param workflowId the pipeline id
   */
  void pruneByWorkflow(String appId, String workflowId);
}
