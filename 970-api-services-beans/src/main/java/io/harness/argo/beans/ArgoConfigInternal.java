package io.harness.argo.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@Builder
@ToString(exclude = "password")
public class ArgoConfigInternal {
  String argoServerUrl;
  String username;
  String password;
  List<EncryptedDataDetail> encryptedDataDetailList;

  boolean isCertValidationRequired;

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getArgoServerUrl() {
    return argoServerUrl.endsWith("/") ? argoServerUrl : argoServerUrl.concat("/");
  }
}
