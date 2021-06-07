package io.harness.delegate.beans.argo.request;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.delegate.task.TaskParameters;

public interface ArgoRequest extends TaskParameters {
  String getAppName();
  RequestType requestType();
//  List<EncryptedDataDetail> getEncryptedDataDetails();
  ArgoConfigInternal getArgoConfigInternal();
}
