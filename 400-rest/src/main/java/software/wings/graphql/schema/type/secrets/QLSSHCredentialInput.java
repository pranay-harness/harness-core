/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSHCredentialInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSSHCredentialInput implements QLObject {
  String name;
  QLSSHAuthenticationScheme authenticationScheme;
  QLSSHAuthenticationInput sshAuthentication;
  QLKerberosAuthenticationInput kerberosAuthentication;
  private QLUsageScope usageScope;
}
