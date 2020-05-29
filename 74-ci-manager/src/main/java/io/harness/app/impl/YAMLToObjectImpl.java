package io.harness.app.impl;

import com.google.inject.Inject;

import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.CIPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class YAMLToObjectImpl implements YAMLToObject {
  @Inject YamlPipelineUtils yamlPipelineUtils;
  @Override
  public CIPipeline convertYAML(String yaml) {
    CIPipeline ciPipeline = null;
    try {
      ciPipeline = yamlPipelineUtils.read(yaml, CIPipeline.class);
    } catch (IOException e) {
      logger.error("Error parsing yaml file", e);
    }
    return ciPipeline;
  }
}
