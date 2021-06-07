package software.wings.delegatetasks.argo.beans.request;

import static software.wings.delegatetasks.argo.beans.request.RequestType.APP_CRUD;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.delegate.beans.argo.ArgoAppConfigDTO;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.settings.argo.ArgoConfig;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgoAppCRUDRequest implements ArgoRequest {
  String appName;
  ArgoAppConfigDTO argoAppConfigDTO;
   List<EncryptedDataDetail> encryptedDataDetails;
  ArgoConfig argoConfig;

  @Override
  public RequestType requestType() {
    return APP_CRUD;
  }
}
