package io.harness.delegate.beans.argo.request;

import io.harness.argo.beans.ArgoConfigInternal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestDiffRequest implements ArgoRequest {
  private String appName;
  private ArgoConfigInternal argoConfigInternal;
  @Override
  public RequestType requestType() {
    return RequestType.MANIFEST_DIFF;
  }
}
