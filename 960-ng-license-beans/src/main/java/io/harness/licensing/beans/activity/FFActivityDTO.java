package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class FFActivityDTO extends LicenseActivityDTO {
  long featureFlagUsers;
  long monthlyActiveUsers;

  public ModuleType getModuleType() {
    return ModuleType.CF;
  }
}
