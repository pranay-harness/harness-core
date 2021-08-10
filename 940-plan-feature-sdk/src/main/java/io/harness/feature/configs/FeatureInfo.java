package io.harness.feature.configs;

import io.harness.ModuleType;
import io.harness.feature.constants.FeatureType;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Data;

@Data
public class FeatureInfo {
  private String name;
  private FeatureType type;
  private Map<Edition, String> restrictions;
  private Map<String, String> overrides;
  private ModuleType moduleType;
}
