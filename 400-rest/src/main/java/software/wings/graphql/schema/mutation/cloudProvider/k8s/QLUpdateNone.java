/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateNone {
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
  private RequestField<String> caCertificateSecretId;
  private RequestField<String> clientCertificateSecretId;
  private RequestField<String> clientKeySecretId;
  private RequestField<String> clientKeyPassphraseSecretId;
  private RequestField<String> clientKeyAlgorithm;
  private RequestField<String> serviceAccountTokenSecretId;
  private RequestField<QLUsageScope> usageScope;
}
