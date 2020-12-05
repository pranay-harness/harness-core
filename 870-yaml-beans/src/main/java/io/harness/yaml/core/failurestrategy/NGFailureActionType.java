package io.harness.yaml.core.failurestrategy;

import io.harness.advisers.retry.RetryAdviser;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NGFailureActionType {
  @JsonProperty(NGFailureActionTypeConstants.IGNORE)
  IGNORE(NGFailureActionTypeConstants.IGNORE, IgnoreAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.RETRY)
  RETRY(NGFailureActionTypeConstants.RETRY, RetryAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.MARK_AS_SUCCESS)
  MARK_AS_SUCCESS(NGFailureActionTypeConstants.MARK_AS_SUCCESS, OnSuccessAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.ABORT)
  ABORT(NGFailureActionTypeConstants.ABORT, OnFailAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.STAGE_ROLLBACK)
  STAGE_ROLLBACK(NGFailureActionTypeConstants.STAGE_ROLLBACK, OnFailAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK)
  STEP_GROUP_ROLLBACK(NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK, OnFailAdviser.ADVISER_TYPE),
  @JsonProperty(NGFailureActionTypeConstants.MANUAL_INTERVENTION)
  MANUAL_INTERVENTION(NGFailureActionTypeConstants.MANUAL_INTERVENTION, ManualInterventionAdviser.ADVISER_TYPE);

  String yamlName;
  AdviserType adviserType;

  NGFailureActionType(String yamlName, AdviserType adviserType) {
    this.yamlName = yamlName;
    this.adviserType = adviserType;
  }

  @JsonCreator
  public static NGFailureActionType getFailureActionType(@JsonProperty("action") String yamlName) {
    for (NGFailureActionType value : NGFailureActionType.values()) {
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

  public AdviserType getAdviserType() {
    return adviserType;
  }
}
