package io.harness.cvng.models;

import lombok.Value;

import java.util.List;

@Value
public class DataSource {
  List<CVConfig> cvConfigs;
}
