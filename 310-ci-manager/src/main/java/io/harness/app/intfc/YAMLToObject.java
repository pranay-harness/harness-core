package io.harness.app.intfc;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;

public interface YAMLToObject {
  NgPipeline convertYAML(String yaml);
}
