package io.harness.delegate.beans.argo.request;

import io.harness.argo.beans.ArgoConfigInternal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArgoAppSyncRequest implements ArgoRequest {
  private String appName;
  ArgoConfigInternal argoConfigInternal;
  @Override
  public RequestType requestType() {
    return RequestType.APP_SYNC;
  }
}
