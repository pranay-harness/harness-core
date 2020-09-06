package io.harness.delegate.beans;

import io.harness.exception.FailureType;
import io.harness.tasks.ErrorResponseData;
import lombok.Builder;
import lombok.Data;

import java.util.EnumSet;

@Data
@Builder
public class ErrorNotifyResponseData implements DelegateTaskNotifyResponseData, ErrorResponseData {
  private EnumSet<FailureType> failureTypes;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
