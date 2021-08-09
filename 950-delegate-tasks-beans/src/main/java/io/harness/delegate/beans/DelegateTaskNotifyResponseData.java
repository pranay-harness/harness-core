package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface DelegateTaskNotifyResponseData extends DelegateResponseData {
  DelegateMetaInfo getDelegateMetaInfo();
  void setDelegateMetaInfo(DelegateMetaInfo metaInfo);
}
