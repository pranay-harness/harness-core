package io.harness.cvng.beans;

import static io.harness.cvng.core.services.CVNextGenConstants.ERRORS_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.RESOURCE_PACK_IDENTIFIER;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CVMonitoringCategory {
  PERFORMANCE(PERFORMANCE_PACK_IDENTIFIER),
  ERRORS(ERRORS_PACK_IDENTIFIER),
  RESOURCES(RESOURCE_PACK_IDENTIFIER);

  private String displayName;

  CVMonitoringCategory(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static CVMonitoringCategory fromDisplayName(String displayName) {
    for (CVMonitoringCategory category : CVMonitoringCategory.values()) {
      if (category.displayName.equalsIgnoreCase(displayName)) {
        return category;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
