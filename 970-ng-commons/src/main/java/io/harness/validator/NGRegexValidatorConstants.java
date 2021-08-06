package io.harness.validator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface NGRegexValidatorConstants {
  String IDENTIFIER_PATTERN = "^[a-zA-Z_][0-9a-zA-Z_$]{0,63}$";
  String NAME_PATTERN = "^[a-zA-Z_][-0-9a-zA-Z_\\s]{0,63}$";
  String TIMEOUT_PATTERN =
      "^(([1-9])+\\d+[s])|(((([1-9])+\\d*[mhwd])+([\\s]?\\d+[smhwd])*)|(<\\+input>.*)|(.*<\\+.*>.*))$";
}
