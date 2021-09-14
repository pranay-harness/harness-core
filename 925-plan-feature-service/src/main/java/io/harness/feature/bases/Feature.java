package io.harness.feature.bases;

import io.harness.ModuleType;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class Feature {
  private FeatureRestriction name;
  private String description;
  private ModuleType moduleType;
  private Map<Edition, Restriction> restrictions;

  public void setModuleType(ModuleType moduleType) {
    this.moduleType = moduleType;
  }
}
