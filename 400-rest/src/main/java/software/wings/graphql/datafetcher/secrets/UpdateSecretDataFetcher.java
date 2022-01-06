/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;
import io.harness.utils.RequestField;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLUpdateSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLUpdateSecretPayload;
import software.wings.graphql.schema.type.secrets.QLEncryptedFileUpdate;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.SecretAuthHandler;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UpdateSecretDataFetcher extends BaseMutatorDataFetcher<QLUpdateSecretInput, QLUpdateSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private EncryptedFileController encryptedFileController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject private SecretAuthHandler secretAuthHandler;

  @Inject
  public UpdateSecretDataFetcher() {
    super(QLUpdateSecretInput.class, QLUpdateSecretPayload.class);
  }

  private SettingAttribute updateSSHCredentials(QLUpdateSecretInput updateSecretInput, String accountId) {
    if (!updateSecretInput.getSshCredential().isPresent()) {
      throw new InvalidRequestException(String.format(
          "No SSH credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    QLSSHCredentialUpdate sshCredential = updateSecretInput.getSshCredential().getValue().orElse(null);
    if (sshCredential == null) {
      throw new InvalidRequestException(String.format(
          "No SSH credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    return sshCredentialController.updateSSHCredential(sshCredential, updateSecretInput.getSecretId(), accountId);
  }

  private SettingAttribute updateWinRMCredential(QLUpdateSecretInput updateSecretInput, String accountId) {
    if (!updateSecretInput.getWinRMCredential().isPresent()) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    QLWinRMCredentialUpdate encryptedTextUpdate = updateSecretInput.getWinRMCredential().getValue().orElse(null);
    if (encryptedTextUpdate == null) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    return winRMCredentialController.updateWinRMCredential(
        encryptedTextUpdate, updateSecretInput.getSecretId(), accountId);
  }

  private EncryptedData updateEncryptedText(QLUpdateSecretInput updateSecretInput, String accountId) {
    if (!updateSecretInput.getEncryptedText().isPresent()) {
      throw new InvalidRequestException(String.format(
          "No encrypted text input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }
    final QLEncryptedTextUpdate encryptedTextUpdate = updateSecretInput.getEncryptedText().getValue().orElseThrow(
        ()
            -> new InvalidRequestException(
                String.format("No encrypted text input provided with the request with secretType %s",
                    updateSecretInput.getSecretType())));

    encryptedTextController.updateEncryptedText(encryptedTextUpdate, updateSecretInput.getSecretId(), accountId);
    return secretManager.getSecretById(accountId, updateSecretInput.getSecretId());
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLUpdateSecretPayload mutateAndFetch(
      QLUpdateSecretInput updateSecretInput, MutationContext mutationContext) {
    QLSecret secret = null;
    switch (updateSecretInput.getSecretType()) {
      case ENCRYPTED_TEXT:
        secretAuthHandler.authorize();
        EncryptedData encryptedText = updateEncryptedText(updateSecretInput, mutationContext.getAccountId());
        secret = encryptedTextController.populateEncryptedText(encryptedText);
        break;
      case WINRM_CREDENTIAL:
        SettingAttribute updatedSettingAttribute =
            updateWinRMCredential(updateSecretInput, mutationContext.getAccountId());
        secret = winRMCredentialController.populateWinRMCredential(updatedSettingAttribute);
        break;
      case SSH_CREDENTIAL:
        SettingAttribute savedSSH = updateSSHCredentials(updateSecretInput, mutationContext.getAccountId());
        secret = sshCredentialController.populateSSHCredential(savedSSH);
        break;
      case ENCRYPTED_FILE:
        secretAuthHandler.authorize();
        DataFetchingEnvironment dataFetchingEnvironment = mutationContext.getDataFetchingEnvironment();
        GraphQLContext context = dataFetchingEnvironment.getContext();
        byte[] bytes = context.get("file");
        EncryptedData encryptedData = updateEncryptedFile(updateSecretInput, mutationContext.getAccountId(), bytes);
        secret = encryptedFileController.populateEncryptedFile(encryptedData);
        break;
      default:
        throw new InvalidRequestException("Invalid Secret Type");
    }
    return QLUpdateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }

  private EncryptedData updateEncryptedFile(QLUpdateSecretInput updateSecretInput, String accountId, byte[] bytes) {
    if (!updateSecretInput.getEncryptedFile().isPresent()) {
      throw new InvalidRequestException(String.format(
          "No encrypted file input provided with the request with secretType %s", updateSecretInput.getSecretType()));
    }

    final QLEncryptedFileUpdate encryptedFileUpdateUpdate = updateSecretInput.getEncryptedFile().getValue().orElseThrow(
        ()
            -> new InvalidRequestException(
                String.format("No encrypted file input provided with the request with secretType %s",
                    updateSecretInput.getSecretType())));

    RequestField<byte[]> fileContent = RequestField.absent();
    if (isNotEmpty(bytes)) {
      fileContent = RequestField.ofNullable(bytes);
    }
    encryptedFileUpdateUpdate.setFileContent(fileContent);
    encryptedFileController.updateEncryptedFile(encryptedFileUpdateUpdate, updateSecretInput.getSecretId(), accountId);
    return secretManager.getSecretById(accountId, updateSecretInput.getSecretId());
  }
}
