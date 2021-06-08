package software.wings.delegatetasks.argo.beans.request;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArgoAppSyncRequest implements ArgoRequest {
  private String appName;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private ArgoConfig argoConfig;
  private String ref;
  @Override
  public RequestType requestType() {
    return RequestType.APP_SYNC;
  }
}
