package io.harness.delegate.task.protocol;

import lombok.Data;

@Data
public abstract class DelegateTaskNotifyResponseData implements ResponseData {
  private DelegateMetaInfo delegateMetaInfo;
}
