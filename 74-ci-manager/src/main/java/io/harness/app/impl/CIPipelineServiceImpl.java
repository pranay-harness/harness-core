package io.harness.app.impl;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.dao.repositories.CIPipelineRepository;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;

@Singleton
public class CIPipelineServiceImpl implements CIPipelineService {
  @Inject private CIPipelineRepository ciPipelineRepository;
  @Inject private CIPipelineValidations ciPipelineValidations;
  @Inject private YAMLToObject yamlToObject;

  @Override
  public CIPipeline createPipelineFromYAML(YAML yaml) {
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml.getPipelineYAML());
    return ciPipelineRepository.save(ciPipeline);
  }

  @Override
  public CIPipeline createPipeline(CIPipeline ciPipeline) {
    ciPipelineValidations.validateCIPipeline(ciPipeline);
    return ciPipelineRepository.save(ciPipeline);
  }

  public CIPipeline readPipeline(String pipelineId) {
    // TODO Validate accountId and fix read pipeline code
    return ciPipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new IllegalArgumentException(format("Pipeline id:%s not found", pipelineId)));
  }
}
