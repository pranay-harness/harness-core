package io.harness.pms.data.stepparameters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSecretSanitizer {
  private static final String SECRET_REGEX = "\\$\\{ngSecretManager.obtain\\((.*)\\)}";
  private static final String SECRET_MASK = "*******";

  public String sanitize(String json) {
    return json.replaceAll(SECRET_REGEX, SECRET_MASK);
  }
}
