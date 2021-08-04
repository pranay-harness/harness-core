package io.harness.yaml.extended.ci.validator;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CI)
public interface ResourceValidatorConstants {
  String MEMORY_PATTERN = "^(([0-9]*[.])?[0-9]+)([GM]i?)$";
}
