package io.harness.perpetualtask.internal;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignmentTaskResponse implements DelegateTaskNotifyResponseData {
  String delegateId;
  DelegateMetaInfo delegateMetaInfo;
  String errorMessage;
  CommandExecutionStatus commandExecutionStatus;
}
