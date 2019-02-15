package io.harness.delegate.task.protocol;

public interface DelegateTaskNotifyResponseData extends ResponseData {
  DelegateMetaInfo getDelegateMetaInfo();
  void setDelegateMetaInfo(DelegateMetaInfo metaInfo);
}
