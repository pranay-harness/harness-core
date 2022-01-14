package io.harness.enforcement.configs;

import io.harness.ModuleType;
import io.harness.enforcement.bases.FeatureRestriction;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeatureRestrictionConfig {
  ModuleType moduleType;
  List<ClientInfo> clients;
  List<FeatureRestriction> features;
}
