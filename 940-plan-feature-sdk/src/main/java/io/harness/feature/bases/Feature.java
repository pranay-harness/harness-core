package io.harness.feature.bases;

import io.harness.feature.interfaces.RestrictionInterface;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Value;

@Value
public class Feature {
  private String name;
  private String description;
  private Map<Edition, RestrictionInterface> restrictions;
}
