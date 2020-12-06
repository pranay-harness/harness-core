package software.wings.service.intfc.ownership;

public interface OwnedByPipeline {
  /**
   * Prune if belongs to pipeline.
   *
   * @param appId the app id
   * @param pipelineId the pipeline id
   */
  void pruneByPipeline(String appId, String pipelineId);
}
