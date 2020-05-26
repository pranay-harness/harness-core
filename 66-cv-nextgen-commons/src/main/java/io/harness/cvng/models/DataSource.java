package io.harness.cvng.models;

import io.harness.cvng.core.services.entities.CVConfig;
import lombok.Value;

import java.util.List;

@Value
public class DataSource {
  List<CVConfig> cvConfigs;
}
