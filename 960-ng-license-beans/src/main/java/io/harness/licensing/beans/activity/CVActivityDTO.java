package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class CVActivityDTO extends LicenseActivityDTO {
  // TBD

  public ModuleType getModuleType() {
    return ModuleType.CV;
  }
}