package io.harness.azure.model.blueprint.assignment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssignmentProvisioningState {
  CANCELED("canceled"),
  CANCELLING("cancelling"),
  CREATING("creating"),
  DELETING("deleting"),
  DEPLOYING("deploying"),
  FAILED("failed"),
  LOCKING("locking"),
  SUCCEEDED("succeeded"),
  VALIDATING("validating"),
  WAITING("waiting");

  private final String value;

  AssignmentProvisioningState(String value) {
    this.value = value;
  }

  /* use this value for deserialization */
  @JsonCreator
  public static AssignmentProvisioningState fromString(String value) {
    AssignmentProvisioningState[] items = AssignmentProvisioningState.values();
    for (AssignmentProvisioningState item : items) {
      if (item.toString().equalsIgnoreCase(value)) {
        return item;
      }
    }
    return null;
  }

  /* use this value for serialization */
  @JsonValue
  public String getValue() {
    return value;
  }
}
