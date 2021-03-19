package io.harness.audit.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YamlDiff {
  YamlRecord oldYaml;
  YamlRecord newYaml;
}
