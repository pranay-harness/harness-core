package io.harness.cvng.core.beans;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.QUALITY_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.RESOURCE_PACK_IDENTIFIER;

public enum CVMonitoringCategory {
  PERFORMANCE(PERFORMANCE_PACK_IDENTIFIER),
  QUALITY(QUALITY_PACK_IDENTIFIER),
  RESOURCES(RESOURCE_PACK_IDENTIFIER);

  private String displayName;

  CVMonitoringCategory(String displayName) {
    this.displayName = displayName;
  }
}
