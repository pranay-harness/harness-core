package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.FailureType;
import io.harness.tasks.ErrorResponseData;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._955_DELEGATE_BEANS)
public class ErrorNotifyResponseData implements DelegateTaskNotifyResponseData, ErrorResponseData {
  private EnumSet<FailureType> failureTypes;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
