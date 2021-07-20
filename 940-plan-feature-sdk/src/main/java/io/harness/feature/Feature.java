package io.harness.feature;

import io.harness.licensing.Edition;
import io.harness.licensing.ModuleType;

import java.util.List;

public class Feature {
  private String name;
  private List<Edition> enabledPlans;
  private List<Restriction> restrictions;
  private List<Restriction> overrides;
  private ModuleType moduleType;
}
