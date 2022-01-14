package io.harness.pms.pipeline.service;

import io.harness.pms.pipeline.PipelineMetadata;

import java.util.Optional;

public interface PipelineMetadataService {
  int incrementExecutionCounter(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier);

  PipelineMetadata save(PipelineMetadata metadata);

  Optional<PipelineMetadata> getMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
