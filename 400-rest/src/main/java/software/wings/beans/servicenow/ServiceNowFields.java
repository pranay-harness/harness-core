package software.wings.beans.servicenow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public enum ServiceNowFields {
  PRIORITY("priority"),
  IMPACT("impact"),
  URGENCY("urgency"),
  RISK("risk"),
  STATE("state"),
  WORK_NOTES("work_notes"),
  DESCRIPTION("description"),
  SHORT_DESCRIPTION("short_description"),
  CHANGE_REQUEST_TYPE("type"),
  CHANGE_REQUEST_NUMBER("change_request"),
  CHANGE_TASK_TYPE("change_task_type");

  @Getter private String jsonBodyName;
  ServiceNowFields(String s) {
    jsonBodyName = s;
  }
}
