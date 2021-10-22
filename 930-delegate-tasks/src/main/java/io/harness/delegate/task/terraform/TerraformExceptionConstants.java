package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public final class TerraformExceptionConstants {
  public TerraformExceptionConstants() {
    throw new UnsupportedOperationException("not supported");
  }

  public static final class Hints {
    public static final String HINT_CHECK_TERRAFORM_CONFIG_LOCATION =
        "Check terraform file '%s' at line '%s' block definition '%s'";
    public static final String HINT_CHECK_TERRAFORM_CONFIG_LOCATION_ARGUMENT =
        "Check terraform file '%s' at line '%s' block definition '%s' argument '%s'";
    public static final String HINT_CHECK_TERRAFORM_CONFIG_FIE = "Check terraform configuration";
  }

  public static final class Explanation {}

  public static final class Message {}

  public static final class CliErrorMessages {
    public static final String NO_VALUE_FOR_REQUIRED_VARIABLE = "no value for required variable";
  }
}
