package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public final class TerraformExceptionConstants {
  public TerraformExceptionConstants() {
    throw new UnsupportedOperationException("not supported");
  }

  public static final class Hints {
    public static final String HINT_CHECK_TERRAFORM_CONFIG = "Check terraform file '%s' at line '%s'";
    public static final String HINT_CHECK_TERRAFORM_CONFIG_LOCATION =
        "Check terraform file '%s' at line '%s' block definition '%s'";
    public static final String HINT_CHECK_TERRAFORM_CONFIG_LOCATION_ARGUMENT =
        "Check terraform file '%s' at line '%s' block definition '%s' argument '%s'";
    public static final String HINT_CHECK_TERRAFORM_CONFIG_FIE = "Check terraform configuration";
    public static final String HINT_INVALID_CREDENTIALS_AWS =
        "Please check access_key and secret_key variables values or AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables";
    public static final String HINT_ERROR_INSPECTING_STATE_IN_BACKEND =
        "Please check your terraform backend configuration";
    public static final String HINT_FAILED_TO_GET_EXISTING_WORKSPACES =
        "Please check your terraform backend configuration";
    public static final String HINT_INVALID_CRED_FOR_S3_BACKEND =
        "Please check access_key and secret_key variables values in Backend Configuration Section or AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables";
  }

  public static final class Explanation {
    public static final String EXPLANATION_INVALID_CREDENTIALS_AWS =
        "Provided credentials for AWS provider may be missing or are invalid";
  }

  public static final class Message {
    public static final String MESSAGE_INVALID_CREDENTIALS_AWS = "Invalid or missing credentials for AWS provider";
    public static final String MESSAGE_ERROR_INSPECTING_STATE_IN_BACKEND = "Failed to initialize terraform backend";
    public static final String MESSAGE_FAILED_TO_GET_EXISTING_WORKSPACES = "Failed to get existing workspaces";
  }

  public static final class CliErrorMessages {
    public static final String FAILED_TO_READ_MODULE_DIRECTORY = "Failed to read module directory";
    public static final String INVALID_CREDENTIALS_AWS =
        "InvalidClientTokenId: The security token included in the request is invalid";
    public static final String ERROR_INSPECTING_STATE_IN_BACKEND = "Error inspecting states in the";
    public static final String FAILED_TO_GET_EXISTING_WORKSPACES = "Failed to get existing workspaces";
    public static final String ERROR_CONFIGURING_S3_BACKEND = "Error configuring S3 Backend";
    public static final String NO_VALID_CRED_FOUND_FOR_S3_BACKEND = "No valid credential sources for S3 Backend found";
    public static final String ERROR_VALIDATING_PROVIDER_CRED = "Error validating provider credentials";
    public static final String NO_VALID_CRED_FOUND_FOR_AWS = "No valid credential sources found for AWS Provider";
  }
}
