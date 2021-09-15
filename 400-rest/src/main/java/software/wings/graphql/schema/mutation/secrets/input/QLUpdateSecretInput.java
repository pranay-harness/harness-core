/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.secrets.input;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedFileUpdate;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateSecretInput implements QLMutationInput {
  String clientMutationId;
  String secretId;
  QLSecretType secretType;
  RequestField<QLEncryptedTextUpdate> encryptedText;
  RequestField<QLWinRMCredentialUpdate> winRMCredential;
  RequestField<QLSSHCredentialUpdate> sshCredential;
  RequestField<QLEncryptedFileUpdate> encryptedFile;
}
