package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeedDataConfiguration {
  @Getter(AccessLevel.NONE) private boolean shouldOverrideAllPredefinedTemplates;

  public boolean shouldOverrideAllPredefinedTemplates() {
    return shouldOverrideAllPredefinedTemplates;
  }
}
