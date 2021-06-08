package software.wings.delegatetasks.argo.beans.request;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.settings.argo.ArgoConfig;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceTreeRequest implements ArgoRequest {
  String appName;
  ArgoConfig argoConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  String activityId;
  String appId;

  @Override
  public RequestType requestType() {
    return RequestType.RESOURCE_TREE;
  }
}
