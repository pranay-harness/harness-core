/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.cloudProvider.k8s;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLManualClusterDetailsAuthenticationType implements QLEnum {
  USERNAME_AND_PASSWORD,
  CLIENT_KEY_AND_CERTIFICATE,
  SERVICE_ACCOUNT_TOKEN,
  OIDC_TOKEN,
  CUSTOM;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
