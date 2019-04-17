package software.wings.beans.servicenow;

import lombok.Getter;

public enum ServiceNowFields {
  PRIORITY("priority"),
  IMPACT("impact"),
  URGENCY("urgency"),
  RISK("risk"),
  STATE("state"),
  WORK_NOTES("work_notes"),
  DESCRIPTION("description"),
  SHORT_DESCRIPTION("short_description"),
  CHANGE_REQUEST_TYPE("type");

  @Getter private String jsonBodyName;
  ServiceNowFields(String s) {
    jsonBodyName = s;
  }
}
