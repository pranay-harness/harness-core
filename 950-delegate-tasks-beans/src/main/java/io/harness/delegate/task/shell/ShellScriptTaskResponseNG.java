package io.harness.delegate.task.shell;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class ShellScriptTaskResponseNG implements DelegateTaskNotifyResponseData {
  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
}
