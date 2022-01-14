package io.harness.pms.pipeline.service;

import io.harness.pms.pipeline.PipelineMetadata;
import io.harness.repositories.pipeline.PipelineMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class PipelineMetadataServiceImpl implements PipelineMetadataService {
  @Inject private PipelineMetadataRepository pipelineMetadataRepository;

  @Override
  public int incrementExecutionCounter(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return pipelineMetadataRepository.incCounter(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier)
        .getRunSequence();
  }

  @Override
  public PipelineMetadata save(PipelineMetadata metadata) {
    return pipelineMetadataRepository.save(metadata);
  }

  @Override
  public Optional<PipelineMetadata> getMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return pipelineMetadataRepository.getPipelineMetadata(accountId, orgIdentifier, projectIdentifier, identifier);
  }
}
