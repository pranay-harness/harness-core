package io.harness.enforcement.bases;

import io.harness.ModuleType;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class FeatureRestriction {
  private FeatureRestrictionName name;
  private String description;
  private ModuleType moduleType;
  private Map<Edition, Restriction> restrictions;

  public void setModuleType(ModuleType moduleType) {
    this.moduleType = moduleType;
  }
}
