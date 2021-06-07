package software.wings.delegatetasks.argo.beans.request;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;

@Data
@Builder
public class ArgoAppSyncRequest implements ArgoRequest {
  private String appName;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private ArgoConfig argoConfig;
  @Override
  public RequestType requestType() {
    return RequestType.APP_SYNC;
  }
}
