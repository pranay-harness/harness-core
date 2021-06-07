package io.harness.delegate.beans.argo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceTreeRequest implements ArgoRequest {
  private String appName;

  @Override
  public RequestType requestType() {
    return RequestType.RESOURCE_TREE;
  }
}
