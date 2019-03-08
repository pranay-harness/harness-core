package io.harness.delegate.beans;

public interface DelegateTaskNotifyResponseData extends ResponseData {
  DelegateMetaInfo getDelegateMetaInfo();
  void setDelegateMetaInfo(DelegateMetaInfo metaInfo);
}
