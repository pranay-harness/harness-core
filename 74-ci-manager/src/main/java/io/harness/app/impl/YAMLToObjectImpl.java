package io.harness.app.impl;

import com.google.common.annotations.VisibleForTesting;

import io.harness.app.intfc.YAMLToObject;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class YAMLToObjectImpl implements YAMLToObject {
  @Override
  public CDPipeline convertYAML(String yaml) {
    CDPipeline ciPipeline = null;
    try {
      ciPipeline = readYaml(yaml);
    } catch (IOException e) {
      logger.error("Error parsing yaml file", e);
    }
    return ciPipeline;
  }

  @VisibleForTesting
  CDPipeline readYaml(String yaml) throws IOException {
    return YamlPipelineUtils.read(yaml, CDPipeline.class);
  }
}
