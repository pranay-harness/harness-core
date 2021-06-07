package io.harness.delegate.beans.argo.request;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResourceTreeRequest implements ArgoRequest {
  private String appName;
  private ArgoConfigInternal argoConfigInternal;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public RequestType requestType() {
    return RequestType.RESOURCE_TREE;
  }
}
