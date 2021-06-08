package software.wings.delegatetasks.argo.beans.request;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestDiffRequest implements ArgoRequest {
  private String appName;
  private ArgoConfig argoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String activityId;
  private String appId;

  @Override
  public RequestType requestType() {
    return RequestType.MANIFEST_DIFF;
  }
}
