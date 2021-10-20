package io.harness.delegate.task.terraform;

import io.harness.annotations.dev.OwnedBy;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
public final class TerraformExceptionConstants {

  public TerraformExceptionConstants() {
    throw new UnsupportedOperationException("not supported");
  }

  public static final class Hints {
    public static final String HINT_NO_VALUE_FOR_REQUIRED_VARIABLE = "Configure terraform var file in terraform step or provide default value";
  }

  public static final class Explanation {
    public static final String EXPLAIN_NO_VALUE_FOR_REQUIRED_VARIABLE = "No value for required input variable(s)";
  }

  public static final class Message {
    public static final String MESSAGE_NO_VALUE_FOR_REQUIRED_VARIABLE =
            "Terraform command '%s' failed. Missing value for one or multiple terraform variables(s)";
  }

  public static final class CliErrorMessages {
    public static final String NO_VALUE_FOR_REQUIRED_VARIABLE = "no value for required variable";
  }
}
