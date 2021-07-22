package io.harness.licensing.beans.activity;

import io.harness.licensing.ModuleType;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class LicenseActivityDTO {
  long timestamp;

  public abstract ModuleType getModuleType();
}
