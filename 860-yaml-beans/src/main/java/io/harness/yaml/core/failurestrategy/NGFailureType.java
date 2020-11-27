package io.harness.yaml.core.failurestrategy;

import static io.harness.exception.FailureType.*;

import io.harness.exception.FailureType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;

public enum NGFailureType {
  @JsonProperty(NGFailureTypeConstants.ALL_ERRORS)
  ALL_ERRORS(NGFailureTypeConstants.ALL_ERRORS,
      EnumSet.of(AUTHENTICATION, CONNECTIVITY, DELEGATE_PROVISIONING, APPLICATION_ERROR,
          FailureType.AUTHORIZATION_ERROR, VERIFICATION_FAILURE, EXPIRED)),
  @JsonProperty(NGFailureTypeConstants.OTHER_ERRORS)
  OTHER_ERRORS(NGFailureTypeConstants.OTHER_ERRORS, EnumSet.of(APPLICATION_ERROR)),
  @JsonProperty(NGFailureTypeConstants.AUTHENTICATION_ERROR)
  AUTHENTICATION_ERROR(NGFailureTypeConstants.AUTHENTICATION_ERROR, EnumSet.of(AUTHENTICATION)),
  @JsonProperty(NGFailureTypeConstants.CONNECTIVITY_ERROR)
  CONNECTIVITY_ERROR(NGFailureTypeConstants.CONNECTIVITY_ERROR, EnumSet.of(CONNECTIVITY)),
  @JsonProperty(NGFailureTypeConstants.TIMEOUT_ERROR)
  TIMEOUT_ERROR(NGFailureTypeConstants.TIMEOUT_ERROR, EnumSet.of(EXPIRED)),
  @JsonProperty(NGFailureTypeConstants.AUTHORIZATION_ERROR)
  AUTHORIZATION_ERROR(NGFailureTypeConstants.AUTHORIZATION_ERROR, EnumSet.of(FailureType.AUTHORIZATION_ERROR)),
  @JsonProperty(NGFailureTypeConstants.VERIFICATION_ERROR)
  VERIFICATION_ERROR(NGFailureTypeConstants.VERIFICATION_ERROR, EnumSet.of(VERIFICATION_FAILURE)),
  @JsonProperty(NGFailureTypeConstants.DELEGATE_PROVISIONING_ERROR)
  DELEGATE_PROVISIONING_ERROR(NGFailureTypeConstants.DELEGATE_PROVISIONING_ERROR, EnumSet.of(DELEGATE_PROVISIONING));

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
