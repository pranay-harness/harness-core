package io.harness.feature.configs;

import io.harness.ModuleType;
import io.harness.feature.bases.Feature;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FeaturesConfig {
  ModuleType moduleType;
  List<ClientInfo> clients;
  List<Feature> features;
}
