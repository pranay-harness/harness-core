package io.harness.app.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import software.wings.dl.WingsPersistence;

@Singleton
public class CIPipelineServiceImpl implements CIPipelineService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private CIPipelineValidations ciPipelineValidations;
  @Inject private YAMLToObject yamlToObject;

  @Override
  public CIPipeline createPipelineFromYAML(YAML yaml) {
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml.getPipelineYAML());
    String ciPipelineKey = wingsPersistence.save(ciPipeline);
    return readPipeline(ciPipelineKey);
  }

  @Override
  public CIPipeline createPipeline(CIPipeline ciPipeline) {
    ciPipelineValidations.validateCIPipeline(ciPipeline);
    String ciPipelineKey = wingsPersistence.save(ciPipeline);
    return readPipeline(ciPipelineKey);
  }

  public CIPipeline readPipeline(String pipelineId) {
    // TODO Validate accountId and fix read pipeline code
    return wingsPersistence.get(CIPipeline.class, pipelineId);
  }
}
