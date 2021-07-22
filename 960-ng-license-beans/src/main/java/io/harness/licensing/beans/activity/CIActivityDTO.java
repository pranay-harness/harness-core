package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class CIActivityDTO extends LicenseActivityDTO {
  long activeCommitters;

  public ModuleType getModuleType() {
    return ModuleType.CI;
  }
}
