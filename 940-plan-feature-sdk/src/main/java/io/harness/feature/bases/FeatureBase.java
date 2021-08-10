package io.harness.feature.bases;

import io.harness.ModuleType;
import io.harness.feature.constants.FeatureType;
import io.harness.licensing.Edition;

import java.util.Map;

public abstract class FeatureBase<T> implements io.harness.feature.interfaces.Feature<T> {
  protected String name;
  protected FeatureType type;
  protected Map<Edition, String> restrictions;
  protected Map<String, String> overrides;
  protected ModuleType moduleType;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public FeatureType getType() {
    return type;
  }

  @Override
  public ModuleType getModuleType() {
    return moduleType;
  }
}
