package io.harness.delegate.beans.argo.request;

import static io.harness.delegate.beans.argo.request.RequestType.APP_CRUD;

import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.delegate.beans.argo.ArgoAppConfigDTO;
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
   ArgoConfigInternal argoConfigInternal;

  @Override
  public RequestType requestType() {
    return APP_CRUD;
  }
}
