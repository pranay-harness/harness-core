package io.harness.servicenow;

import lombok.Getter;

public enum ServiceNowTicketTypeNG {
  INCIDENT("Incident"),
  PROBLEM("Problem"),
  CHANGE_REQUEST("Change"),
  CHANGE_TASK("Change Task");
  @Getter private String displayName;
  ServiceNowTicketTypeNG(String s) {
    displayName = s;
  }
}
