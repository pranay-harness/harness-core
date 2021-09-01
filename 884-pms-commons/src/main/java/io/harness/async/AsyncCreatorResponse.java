package io.harness.async;

import io.harness.pms.yaml.YamlField;

import java.util.List;
import java.util.Map;

public interface AsyncCreatorResponse {
  Map<String, YamlField> getDependencies();

  List<String> getErrorMessages();
}
