package io.harness.ccm.config;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CCMConfig {
  boolean cloudCostEnabled;
  boolean skipK8sEventCollection;
}
