package io.harness.licensing;

public enum EditionAction {
  START_FREE(""),
  START_TRIAL(""),
  EXTEND_TRIAL(""),
  SUBSCRIBE(""),
  UPGRADE(""),
  CONTACT_SALES(""),
  CONTACT_SUPPORT(""),
  MANAGE(""),
  DISABLED_BY_TEAM("Team plan is subscribed by other module"),
  DISABLED_BY_ENTERPRISE("Enterprise plan is subscribed by other module");

  private String reason;

  EditionAction(String reason) {
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }
}
