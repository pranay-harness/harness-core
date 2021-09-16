package io.harness.feature.configs;

import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Value;

@Value
public class FeatureInfo {
  private String name;
  private String description;
  private Map<Edition, RestrictionConfig> restrictions;
  private Map<String, String> overrides;
}
