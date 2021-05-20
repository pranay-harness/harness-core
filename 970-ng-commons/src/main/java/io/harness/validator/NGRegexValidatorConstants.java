package io.harness.validator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface NGRegexValidatorConstants {
  public static final String IDENTIFIER_PATTERN = "^[a-zA-Z_][0-9a-zA-Z_$]{0,63}$";
  public static final String NAME_PATTERN = "^[a-zA-Z_][-0-9a-zA-Z_\\s]{0,63}$";
}
