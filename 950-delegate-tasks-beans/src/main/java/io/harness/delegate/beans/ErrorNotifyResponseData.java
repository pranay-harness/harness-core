package io.harness.delegate.beans;

import io.harness.exception.FailureType;
import io.harness.tasks.ErrorResponseData;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorNotifyResponseData implements DelegateTaskNotifyResponseData, ErrorResponseData {
  private EnumSet<FailureType> failureTypes;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
