package software.wings.beans.trigger;

import io.harness.exception.InvalidRequestException;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseAction {
  CREATED("Created", "created"),
  PUBLISHED("Published", "published"),
  RELEASED("released", "released"),
  UNPUBLISHED("Unpublished", "unpublished"),
  EDITED("Edited", "edited"),
  DELETED("Deleted", "deleted"),
  PRE_RELEASED("Pre-Released", "prereleased");

  private String displayName;
  private String value;

  ReleaseAction(String displayName, String value) {
    this.displayName = displayName;
    this.value = value;
    ReleaseActionHolder.map.put(value, this);
  }

  private static class ReleaseActionHolder { static Map<String, ReleaseAction> map = new HashMap<>(); }

  public static ReleaseAction find(String val) {
    ReleaseAction releaseAction = ReleaseActionHolder.map.get(val);
    if (releaseAction == null) {
      throw new InvalidRequestException(String.format("Unsupported Release action %s.", val));
    }
    return releaseAction;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getValue() {
    return value;
  }
}
