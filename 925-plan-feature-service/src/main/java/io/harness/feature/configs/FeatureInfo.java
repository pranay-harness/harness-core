package io.harness.feature.configs;

import io.harness.feature.bases.Restriction;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Value;

@Value
public class FeatureInfo {
  private FeatureRestriction name;
  private String description;
  private Map<Edition, Restriction> restrictions;
  private Map<String, String> overrides;
}
