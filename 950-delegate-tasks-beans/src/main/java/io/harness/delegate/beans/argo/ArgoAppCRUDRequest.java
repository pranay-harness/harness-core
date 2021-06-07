package io.harness.delegate.beans.argo;

import static io.harness.delegate.beans.argo.RequestType.APP_CRUD;

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

  @Override
  public RequestType requestType() {
    return APP_CRUD;
  }
}
