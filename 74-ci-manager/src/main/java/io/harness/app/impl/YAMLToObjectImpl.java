package io.harness.app.impl;

import com.google.common.annotations.VisibleForTesting;

import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.CIPipeline;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class YAMLToObjectImpl implements YAMLToObject {
  @Override
  public CIPipeline convertYAML(String yaml) {
    CIPipeline ciPipeline = null;
    try {
      ciPipeline = readYaml(yaml);
    } catch (IOException e) {
      logger.error("Error parsing yaml file", e);
    }
    return ciPipeline;
  }

  @VisibleForTesting
  CIPipeline readYaml(String yaml) throws IOException {
    return YamlPipelineUtils.read(yaml, CIPipeline.class);
  }
}
