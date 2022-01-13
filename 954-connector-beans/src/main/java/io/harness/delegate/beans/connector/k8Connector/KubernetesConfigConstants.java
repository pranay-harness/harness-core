/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.k8Connector;

public class KubernetesConfigConstants {
  // auth type
  public static final String USERNAME_PASSWORD = "UsernamePassword";
  public static final String CLIENT_KEY_CERT = "ClientKeyCert";
  public static final String SERVICE_ACCOUNT = "ServiceAccount";
  public static final String OPENID_CONNECT = "OpenIdConnect";

  public static final String INHERIT_FROM_DELEGATE = "InheritFromDelegate";
  public static final String MANUAL_CREDENTIALS = "ManualConfig";
}
