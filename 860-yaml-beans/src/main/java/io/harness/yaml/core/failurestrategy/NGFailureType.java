package io.harness.yaml.core.failurestrategy;

import static io.harness.pms.execution.failure.FailureType.*;

import io.harness.pms.execution.failure.FailureType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;

public enum NGFailureType {
  @JsonProperty(NGFailureTypeConstants.ALL_ERRORS)
  ALL_ERRORS(NGFailureTypeConstants.ALL_ERRORS,
      EnumSet.of(AUTHENTICATION_FAILURE, CONNECTIVITY_FAILURE, DELEGATE_PROVISIONING_FAILURE, APPLICATION_FAILURE,
          AUTHORIZATION_FAILURE, VERIFICATION_FAILURE, TIMEOUT_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.OTHER_ERRORS)
  OTHER_ERRORS(NGFailureTypeConstants.OTHER_ERRORS, EnumSet.of(APPLICATION_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.AUTHENTICATION_ERROR)
  AUTHENTICATION_ERROR(NGFailureTypeConstants.AUTHENTICATION_ERROR, EnumSet.of(AUTHENTICATION_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.CONNECTIVITY_ERROR)
  CONNECTIVITY_ERROR(NGFailureTypeConstants.CONNECTIVITY_ERROR, EnumSet.of(CONNECTIVITY_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.TIMEOUT_ERROR)
  TIMEOUT_ERROR(NGFailureTypeConstants.TIMEOUT_ERROR, EnumSet.of(TIMEOUT_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.AUTHORIZATION_ERROR)
  AUTHORIZATION_ERROR(NGFailureTypeConstants.AUTHORIZATION_ERROR, EnumSet.of(AUTHORIZATION_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.VERIFICATION_ERROR)
  VERIFICATION_ERROR(NGFailureTypeConstants.VERIFICATION_ERROR, EnumSet.of(VERIFICATION_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.DELEGATE_PROVISIONING_ERROR)
  DELEGATE_PROVISIONING_ERROR(
      NGFailureTypeConstants.DELEGATE_PROVISIONING_ERROR, EnumSet.of(DELEGATE_PROVISIONING_FAILURE));

  private final String yamlName;
  private final EnumSet<FailureType> failureType;

  NGFailureType(String yamlName, EnumSet<FailureType> failureType) {
    this.yamlName = yamlName;
    this.failureType = failureType;
  }

  @JsonCreator
  public static NGFailureType getFailureType(String yamlName) {
    for (NGFailureType value : NGFailureType.values()) {
      if (value.yamlName.equalsIgnoreCase(yamlName)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  public EnumSet<FailureType> getFailureType() {
    return failureType;
  }
}
