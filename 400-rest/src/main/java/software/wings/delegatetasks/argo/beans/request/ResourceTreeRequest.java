package software.wings.delegatetasks.argo.beans.request;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;

@Data
@Builder
public class ResourceTreeRequest implements ArgoRequest {
  private String appName;
  private ArgoConfig argoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public RequestType requestType() {
    return RequestType.RESOURCE_TREE;
  }
}
