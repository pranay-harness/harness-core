/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.secrets;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWinRMCredentialsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDP)
public class QLWinRMCredential implements QLSecret {
  private String id;
  private String name;
  private QLSecretType secretType;
  private QLAuthScheme authenticationScheme;
  private String userName;
  private Boolean useSSL;
  private String domain;
  private Boolean skipCertCheck;
  private Integer port;
  private QLUsageScope usageScope;
  private String keyTabFilePath;
}
