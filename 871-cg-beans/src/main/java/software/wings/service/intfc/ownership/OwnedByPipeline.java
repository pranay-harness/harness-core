package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OwnedByPipeline {
  /**
   * Prune if belongs to pipeline.
   *
   * @param appId the app id
   * @param pipelineId the pipeline id
   */
  void pruneByPipeline(String appId, String pipelineId);
}
