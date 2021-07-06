package io.harness.yaml.validator;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CI)
public interface ResourceValidatorConstants {
  String MEMORY_PATTERN = "^(\\d+)([GM]i?)$";
}
