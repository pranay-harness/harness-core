package software.wings.delegatetasks.argo.beans.request;

import static software.wings.delegatetasks.argo.beans.request.RequestType.APP_CRUD;

import io.harness.delegate.beans.argo.ArgoAppConfigDTO;
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
public class ArgoAppCRUDRequest implements ArgoRequest {
  String appName;
  ArgoAppConfigDTO argoAppConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  ArgoConfig argoConfig;
  String activityId;
  String appId;

  @Override
  public RequestType requestType() {
    return APP_CRUD;
  }
}
