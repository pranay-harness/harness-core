/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
