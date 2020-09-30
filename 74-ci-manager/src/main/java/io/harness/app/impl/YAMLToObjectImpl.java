package io.harness.app.impl;

import com.google.common.annotations.VisibleForTesting;

import io.harness.app.intfc.YAMLToObject;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class YAMLToObjectImpl implements YAMLToObject {
  @Override
  public NgPipeline convertYAML(String yaml) {
    NgPipeline ngPipeline = null;
    try {
      ngPipeline = readYaml(yaml);
    } catch (IOException e) {
      logger.error("Error parsing yaml file", e);
    }
    return ngPipeline;
  }

  @VisibleForTesting
  NgPipeline readYaml(String yaml) throws IOException {
    return YamlPipelineUtils.read(yaml, NgPipeline.class);
  }
}
