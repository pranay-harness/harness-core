package io.harness.ccm.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CCMConfig {
  boolean cloudCostEnabled;
  boolean skipK8sEventCollection;
}
