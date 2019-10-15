package io.harness.ccm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CCMConfig {
  boolean cloudCostEnabled;
}
