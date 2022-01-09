/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLCreateSecretPayload;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.SecretAuthHandler;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class CreateSecretDataFetcher extends BaseMutatorDataFetcher<QLCreateSecretInput, QLCreateSecretPayload> {
  public static final String FILE = "file";
  @Inject private SecretManager secretManager;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private EncryptedFileController encryptedFileController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject private SettingsService settingsService;
  @Inject private SecretAuthHandler secretAuthHandler;

  @Inject
  public CreateSecretDataFetcher(SecretManager secretManager) {
    super(QLCreateSecretInput.class, QLCreateSecretPayload.class);
    this.secretManager = secretManager;
  }

  private SettingAttribute saveSSHCredential(QLCreateSecretInput input, String accountId) {
    if (input.getSshCredential() == null) {
      throw new InvalidRequestException(
          String.format("No ssh credential input provided with the request with secretType %s", input.getSecretType()));
    }
    SettingAttribute settingAttribute =
        sshCredentialController.createSettingAttribute(input.getSshCredential(), accountId);
    return settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
  }

  private SettingAttribute saveWinRMCredential(QLCreateSecretInput input, String accountId) {
    if (input.getWinRMCredential() == null) {
      throw new InvalidRequestException(String.format(
          "No winRM credential input provided with the request with secretType %s", input.getSecretType()));
    }
    SettingAttribute settingAttribute =
        winRMCredentialController.createSettingAttribute(input.getWinRMCredential(), accountId);
    return settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
  }

  private EncryptedData saveEncryptedText(QLCreateSecretInput input, String accountId) {
    if (input.getEncryptedText() == null) {
      throw new InvalidRequestException(
          String.format("No encrypted text input provided with the request with secretType %s", input.getSecretType()));
    }
    String secretId = encryptedTextController.createEncryptedText(input, accountId);
    return secretManager.getSecretById(accountId, secretId);
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLCreateSecretPayload mutateAndFetch(QLCreateSecretInput input, MutationContext mutationContext) {
    QLSecret secret = null;
    switch (input.getSecretType()) {
      case ENCRYPTED_TEXT:
        secretAuthHandler.authorize();
        EncryptedData encryptedText = saveEncryptedText(input, mutationContext.getAccountId());
        secret = encryptedTextController.populateEncryptedText(encryptedText);
        break;
      case WINRM_CREDENTIAL:
        SettingAttribute savedSettingAttribute = saveWinRMCredential(input, mutationContext.getAccountId());
        secret = winRMCredentialController.populateWinRMCredential(savedSettingAttribute);
        break;
      case SSH_CREDENTIAL:
        SettingAttribute savedSSH = saveSSHCredential(input, mutationContext.getAccountId());
        secret = sshCredentialController.populateSSHCredential(savedSSH);
        break;
      case ENCRYPTED_FILE:
        secretAuthHandler.authorize();
        DataFetchingEnvironment dataFetchingEnvironment = mutationContext.getDataFetchingEnvironment();
        GraphQLContext context = dataFetchingEnvironment.getContext();
        byte[] bytes = context.get(FILE);
        if (isEmpty(bytes)) {
          throw new InvalidRequestException("No/Empty file uploaded.");
        }
        EncryptedData encryptedData = saveEncryptedFile(input, mutationContext.getAccountId(), bytes);
        secret = encryptedFileController.populateEncryptedFile(encryptedData);
        break;
      default:
        throw new InvalidRequestException("Invalid secret Type");
    }
    return QLCreateSecretPayload.builder().clientMutationId(mutationContext.getAccountId()).secret(secret).build();
  }

  private EncryptedData saveEncryptedFile(QLCreateSecretInput input, String accountId, byte[] bytes) {
    if (input.getEncryptedFile() == null) {
      throw new InvalidRequestException(
          String.format("No encrypted file input provided with the request with secretType %s", input.getSecretType()));
    }
    input.getEncryptedFile().setFileContent(bytes);
    String secretId = encryptedFileController.createEncryptedFile(input, accountId);
    return secretManager.getSecretById(accountId, secretId);
  }
}
