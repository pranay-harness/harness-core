package io.harness.feature.bases;

import io.harness.ModuleType;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
public class Feature {
  private String name;
  private String description;
  private ModuleType moduleType;
  private Map<Edition, Restriction> restrictions;
}
