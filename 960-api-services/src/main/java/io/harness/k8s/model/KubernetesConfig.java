/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@ToString(exclude = {"password", "caCert", "clientCert", "clientKey", "clientKeyPassphrase", "serviceAccountToken"})
public class KubernetesConfig {
  @NotEmpty private String masterUrl;
  private char[] username;
  private char[] password;
  private char[] caCert;
  private char[] clientCert;
  private char[] clientKey;
  private char[] clientKeyPassphrase;
  private char[] serviceAccountToken;
  private String clientKeyAlgo;
  private String namespace;
  @NotEmpty private String accountId;

  private KubernetesClusterAuthType authType;
  // -- OIDC AUTH fields.
  private String oidcIdentityProviderUrl;
  private String oidcUsername;
  private OidcGrantType oidcGrantType;
  private String oidcScopes;
  private char[] oidcSecret;
  private char[] oidcClientId;
  private char[] oidcPassword;

  @Builder
  public KubernetesConfig(String masterUrl, char[] username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, char[] clientKeyPassphrase, char[] serviceAccountToken, String clientKeyAlgo, String namespace,
      String accountId, KubernetesClusterAuthType authType, char[] oidcClientId, char[] oidcSecret,
      String oidcIdentityProviderUrl, String oidcUsername, char[] oidcPassword, String oidcScopes,
      OidcGrantType oidcGrantType) {
    this.masterUrl = masterUrl;
    this.username = username == null ? null : username.clone();
    this.password = password == null ? null : password.clone();
    this.caCert = caCert == null ? null : caCert.clone();
    this.clientCert = clientCert == null ? null : clientCert.clone();
    this.clientKey = clientKey == null ? null : clientKey.clone();
    this.clientKeyPassphrase = clientKeyPassphrase == null ? null : clientKeyPassphrase.clone();
    this.serviceAccountToken = serviceAccountToken == null ? null : serviceAccountToken.clone();
    this.clientKeyAlgo = clientKeyAlgo;
    this.namespace = namespace;
    this.accountId = accountId;
    this.authType = authType;
    this.oidcClientId = oidcClientId == null ? null : oidcClientId.clone();
    this.oidcSecret = oidcSecret == null ? null : oidcSecret.clone();
    this.oidcIdentityProviderUrl = oidcIdentityProviderUrl;
    this.oidcUsername = oidcUsername;
    this.oidcPassword = oidcPassword == null ? null : oidcPassword.clone();
    this.oidcScopes = oidcScopes;
    this.oidcGrantType = oidcGrantType == null ? OidcGrantType.password : oidcGrantType;
  }
}
