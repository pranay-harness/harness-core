package io.harness.argo.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "password")
public class ArgoConfigInternal {
  String argoServerUrl;
  String username;
  String password;
  boolean isCertValidationRequired;

  public boolean hasCredentials() {
    return isNotEmpty(username);
  }

  public String getArgoServerUrl() {
    return argoServerUrl.endsWith("/") ? argoServerUrl : argoServerUrl.concat("/");
  }
}
