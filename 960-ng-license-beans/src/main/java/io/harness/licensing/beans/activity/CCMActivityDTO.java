package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class CCMActivityDTO extends LicenseActivityDTO {
  long activeSpend;

  public ModuleType getModuleType() {
    return ModuleType.CE;
  }
}