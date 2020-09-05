package io.harness.delegate.beans;

public interface DelegateTaskNotifyResponseData extends DelegateResponseData {
  DelegateMetaInfo getDelegateMetaInfo();
  void setDelegateMetaInfo(DelegateMetaInfo metaInfo);
}
