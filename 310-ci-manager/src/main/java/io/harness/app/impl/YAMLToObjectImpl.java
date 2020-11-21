package io.harness.app.impl;

import io.harness.app.intfc.YAMLToObject;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YAMLToObjectImpl implements YAMLToObject {
  @Override
  public NgPipeline convertYAML(String yaml) {
    NgPipeline ngPipeline = null;
    try {
      ngPipeline = readYaml(yaml);
    } catch (IOException e) {
      log.error("Error parsing yaml file", e);
    }
    return ngPipeline;
  }

  @VisibleForTesting
  NgPipeline readYaml(String yaml) throws IOException {
    return YamlPipelineUtils.read(yaml, NgPipeline.class);
  }
}
