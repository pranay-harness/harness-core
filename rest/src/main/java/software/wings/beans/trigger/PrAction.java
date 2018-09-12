package software.wings.beans.trigger;

import io.harness.exception.WingsException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 8/5/18.
 */
public enum PrAction {
  CLOSED("Closed", "closed"),
  EDITED("Edited", "edited"),
  OPENED("Opened", "opened"),
  REOPENED("Reopened", "reopened"),
  ASSIGNED("Assigned", "assigned"),
  UNASSIGNED("Unassigned", "unassigned"),
  LABELED("Labeled", "labeled"),
  UNLABELED("Unlabeled", "unlabeled"),
  SYNCHRONIZED("Synchronized", "synchronized"),
  REVIEW_REQUESTED("Review Requested", "review_requested"),
  REVIEW_REQUESTED_REMOVED("Review Request Removed", "review_request_removed");

  private String displayName;
  private String value;

  PrAction(String displayName, String value) {
    this.displayName = displayName;
    this.value = value;
    PrActionHolder.map.put(value, this);
  }

  private static class PrActionHolder { static Map<String, PrAction> map = new HashMap<>(); }

  public static PrAction find(String val) {
    PrAction prAction = PrActionHolder.map.get(val);
    if (prAction == null) {
      throw new WingsException(String.format("Unsupported Pull Request action %s.", val));
    }
    return prAction;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getValue() {
    return value;
  }
}